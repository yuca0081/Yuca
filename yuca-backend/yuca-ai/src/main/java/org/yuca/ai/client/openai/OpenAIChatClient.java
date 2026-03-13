package org.yuca.ai.client.openai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.yuca.ai.client.openai.dto.OpenAIChatRequest;
import org.yuca.ai.client.openai.dto.OpenAIChatResponse;
import org.yuca.ai.client.openai.dto.OpenAIMessage;
import org.yuca.ai.model.ChatRequest;
import org.yuca.ai.client.AIChatClient;
import org.yuca.ai.model.AIMessage;
import org.yuca.ai.model.ChatResponse;
import org.yuca.ai.model.MessageRole;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.function.Consumer;

/**
 * OpenAI 实现
 *
 * @author Yuca
 * @since 2025-01-27
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "openai.api-key")
public class OpenAIChatClient implements AIChatClient {

    private final RestTemplate restTemplate;
    private final OpenAIConfig config;
    private final ObjectMapper objectMapper;

    private static final String CHAT_COMPLETIONS_ENDPOINT = "/chat/completions";

    @Override
    public ChatResponse chat(ChatRequest request) {
        long startTime = System.currentTimeMillis();

        OpenAIChatRequest openaiRequest = buildRequest(request, false);
        logRequest(request, openaiRequest);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(config.getApiKey());

        HttpEntity<OpenAIChatRequest> entity = new HttpEntity<>(openaiRequest, headers);

        try {
            ResponseEntity<OpenAIChatResponse> responseEntity = restTemplate.postForEntity(
                config.getBaseUrl() + CHAT_COMPLETIONS_ENDPOINT,
                entity,
                OpenAIChatResponse.class
            );

            ChatResponse response = parseResponse(responseEntity.getBody());
            long duration = System.currentTimeMillis() - startTime;
            logResponse(response, duration);

            return response;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("OpenAI 调用失败，耗时: {}ms", duration, e);
            throw new RuntimeException("AI 调用失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void chatStream(ChatRequest request, Consumer<String> tokenHandler) {
        long startTime = System.currentTimeMillis();

        OpenAIChatRequest openaiRequest = buildRequest(request, true);
        logRequest(request, openaiRequest);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(config.getApiKey());

            HttpEntity<OpenAIChatRequest> entity = new HttpEntity<>(openaiRequest, headers);

            // 使用 execute 方法处理流式响应
            restTemplate.execute(
                config.getBaseUrl() + CHAT_COMPLETIONS_ENDPOINT,
                org.springframework.http.HttpMethod.POST,
                requestCallback -> {
                    requestCallback.getHeaders().putAll(headers);
                    requestCallback.getBody().write(new ObjectMapper().writeValueAsBytes(openaiRequest));
                },
                responseExtractor -> {
                    // 直接读取响应流
                    try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(responseExtractor.getBody(), "UTF-8"))) {

                        String line;
                        while ((line = reader.readLine()) != null) {
                            String content = parseSseLine(line);
                            if (content != null && !content.isEmpty()) {
                                tokenHandler.accept(content);
                            }
                        }
                    }
                    long duration = System.currentTimeMillis() - startTime;
                    log.info("OpenAI 流式调用完成，耗时: {}ms", duration);
                    return null;
                }
            );

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("OpenAI 流式调用失败，耗时: {}ms", duration, e);
            throw new RuntimeException("AI 调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解析 SSE 行
     */
    private String parseSseLine(String data) {
        try {
            if (data.startsWith("data: ")) {
                String json = data.substring(6).trim();
                if ("[DONE]".equals(json)) {
                    return null;
                }
                JsonNode jsonNode = objectMapper.readTree(json);
                JsonNode choicesNode = jsonNode.get("choices");

                if (choicesNode != null && choicesNode.isArray() && choicesNode.size() > 0) {
                    JsonNode deltaNode = choicesNode.get(0).get("delta");
                    if (deltaNode != null && deltaNode.has("content")) {
                        return deltaNode.get("content").asText();
                    }
                }
            }
            return null;
        } catch (Exception e) {
            log.warn("解析流式响应行失败: {}, 错误: {}", data, e.getMessage());
            return null;
        }
    }

    /**
     * 构建 OpenAI 请求
     */
    private OpenAIChatRequest buildRequest(ChatRequest request, boolean stream) {
        List<OpenAIMessage> openaiMessages = request.getMessages().stream()
            .map(this::convertMessage)
            .toList();

        return OpenAIChatRequest.builder()
            .model(request.getModel() != null ? request.getModel() : config.getModel())
            .messages(openaiMessages)
            .temperature(request.getTemperature() != null ? request.getTemperature() : Double.valueOf(config.getTemperature()))
            .max_tokens(request.getMaxTokens() != null ? request.getMaxTokens() : config.getMaxTokens())
            .stream(stream)
            .build();
    }

    /**
     * 转换消息格式
     */
    private OpenAIMessage convertMessage(AIMessage message) {
        return OpenAIMessage.builder()
            .role(convertRole(message.getRole()))
            .content(message.getContent())
            .build();
    }

    /**
     * 转换角色
     */
    private String convertRole(MessageRole role) {
        return switch (role) {
            case USER -> "user";
            case ASSISTANT -> "assistant";
            case SYSTEM -> "system";
            case TOOL -> "tool";
        };
    }

    /**
     * 解析响应
     */
    private ChatResponse parseResponse(OpenAIChatResponse openaiResponse) {
        if (openaiResponse == null || openaiResponse.getChoices() == null || openaiResponse.getChoices().isEmpty()) {
            throw new RuntimeException("AI 返回空结果");
        }

        OpenAIChatResponse.Choice choice = openaiResponse.getChoices().get(0);
        String content = choice.getMessage().getContent();

        ChatResponse.Usage usage = null;
        if (openaiResponse.getUsage() != null) {
            usage = ChatResponse.Usage.builder()
                .inputTokens(openaiResponse.getUsage().getPrompt_tokens())
                .outputTokens(openaiResponse.getUsage().getCompletion_tokens())
                .build();
        }

        return ChatResponse.builder()
            .content(content)
            .usage(usage)
            .requestId(openaiResponse.getId())
            .model(openaiResponse.getModel())
            .build();
    }

    /**
     * 记录请求日志
     */
    private void logRequest(ChatRequest request, OpenAIChatRequest openaiRequest) {
        log.info("========== AI 调用开始 ==========");
        log.info("提供商: OpenAI");
        log.info("API 地址: {}", config.getBaseUrl());
        log.info("模型: {}", openaiRequest.getModel());
        log.info("消息数: {}", openaiRequest.getMessages().size());

        if (log.isDebugEnabled()) {
            for (int i = 0; i < openaiRequest.getMessages().size(); i++) {
                OpenAIMessage msg = openaiRequest.getMessages().get(i);
                String preview = msg.getContent().length() > 50
                    ? msg.getContent().substring(0, 50) + "..."
                    : msg.getContent();
                log.debug("消息[{}]: role={}, content={}", i, msg.getRole(), preview);
            }
        }
    }

    /**
     * 记录响应日志
     */
    private void logResponse(ChatResponse response, long duration) {
        log.info("AI 调用成功，耗时: {}ms", duration);
        log.info("内容长度: {}", response.getContent().length());

        if (response.getUsage() != null) {
            log.info("Token 使用: input={}, output={}, total={}",
                response.getUsage().getInputTokens(),
                response.getUsage().getOutputTokens(),
                response.getUsage().getTotalTokens()
            );
        }

        if (log.isDebugEnabled()) {
            String preview = response.getContent().length() > 100
                ? response.getContent().substring(0, 100) + "..."
                : response.getContent();
            log.debug("回复内容: {}", preview);
        }

        log.info("========== AI 调用结束 ==========");
    }
}
