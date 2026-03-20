package org.yuca.ai.client.qwen;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.yuca.ai.client.AIChatClient;
import org.yuca.ai.client.qwen.dto.QwenChatRequest;
import org.yuca.ai.model.ChatRequest;
import org.yuca.ai.model.StreamToken;
import org.yuca.ai.model.ChatResponse;
import org.yuca.ai.model.ChatStreamResponse;
import org.yuca.ai.tool.AITool;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * 通义千问 AI 聊天客户端
 * <p>
 * 使用 Builder 模式创建，支持配置默认 tools
 *
 * @author Yuca
 * @since 2025-01-27
 */
@Slf4j
public class QwenChatClient implements AIChatClient<QwenChatRequest> {

    private final QwenConfig config;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final List<AITool> defaultTools;

    private static final String CHAT_COMPLETIONS_ENDPOINT = "/chat/completions";

    private QwenChatClient(Builder builder) {
        this.config = builder.config;
        this.restTemplate = builder.restTemplate;
        this.objectMapper = builder.objectMapper;
        this.defaultTools = builder.defaultTools;
    }

    /**
     * 创建 Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 聊天对话（非流式）
     */
    @Override
    public ChatResponse chat(QwenChatRequest request) {
        long startTime = System.currentTimeMillis();

        // 应用默认 tools
        final QwenChatRequest processedRequest = applyDefaultTools(request);

        // 补充默认值
        completeRequest(processedRequest, false);
        logRequest(processedRequest);

        HttpHeaders headers = createHeaders();
        HttpEntity<QwenChatRequest> entity = new HttpEntity<>(processedRequest, headers);

        try {
            ResponseEntity<ChatResponse> responseEntity = restTemplate.postForEntity(
                config.getBaseUrl() + CHAT_COMPLETIONS_ENDPOINT,
                entity,
                ChatResponse.class
            );

            ChatResponse response = responseEntity.getBody();
            long duration = System.currentTimeMillis() - startTime;
            logResponse(response, duration);

            return response;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("千问调用失败，耗时: {}ms", duration, e);
            throw new RuntimeException("AI 调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * 聊天对话（流式）
     */
    @Override
    public ChatStreamResponse chatStream(QwenChatRequest request, Consumer<StreamToken> tokenHandler) {
        long startTime = System.currentTimeMillis();

        // 应用默认 tools
        final QwenChatRequest processedRequest = applyDefaultTools(request);

        // 补充默认值
        completeRequest(processedRequest, true);
        logRequest(processedRequest);

        final ChatStreamResponse[] responseRef = new ChatStreamResponse[1];
        HttpHeaders headers = createHeaders();

        try {
            restTemplate.execute(
                config.getBaseUrl() + CHAT_COMPLETIONS_ENDPOINT,
                org.springframework.http.HttpMethod.POST,
                requestCallback -> {
                    requestCallback.getHeaders().putAll(headers);
                    requestCallback.getBody().write(objectMapper.writeValueAsBytes(processedRequest));
                },
                responseExtractor -> {
                    try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(responseExtractor.getBody(), "UTF-8"))) {

                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (line.startsWith("data: ")) {
                                String json = line.substring(6).trim();
                                if ("[DONE]".equals(json)) {
                                    break;
                                }

                                try {
                                    ChatStreamResponse chunkResponse = objectMapper.readValue(json, ChatStreamResponse.class);

                                    if (chunkResponse.getUsage() != null) {
                                        responseRef[0] = chunkResponse;
                                    }

                                    StreamToken token = chunkResponse.toStreamToken();
                                    if (token != null && token.getContent() != null && !token.getContent().isEmpty()) {
                                        tokenHandler.accept(token);
                                    }
                                } catch (Exception e) {
                                    log.warn("解析流式响应chunk失败: {}, 错误: {}", line, e.getMessage());
                                }
                            }
                        }
                    }
                    long duration = System.currentTimeMillis() - startTime;
                    log.info("千问流式调用完成，耗时: {}ms", duration);
                    return null;
                }
            );

            return responseRef[0] != null ? responseRef[0] : ChatStreamResponse.builder().build();

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("千问流式调用失败，耗时: {}ms", duration, e);
            throw new RuntimeException("AI 调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * 应用默认 tools
     */
    private QwenChatRequest applyDefaultTools(QwenChatRequest request) {
        if (!defaultTools.isEmpty() && (request.getTools() == null || request.getTools().isEmpty())) {
            request.setTools(convertToChatRequestTools(defaultTools));
        }
        return request;
    }

    /**
     * 转换 AITool 到 ChatRequest.Tool
     */
    private List<ChatRequest.Tool> convertToChatRequestTools(List<AITool> aiTools) {
        List<ChatRequest.Tool> tools = new ArrayList<>();
        for (AITool aiTool : aiTools) {
            tools.add(ChatRequest.Tool.builder()
                .type("function")
                .function(ChatRequest.Tool.Function.builder()
                    .name(aiTool.getName())
                    .description(aiTool.getDescription())
                    .parameters(aiTool.getParameters().toMap())
                    .build())
                .build());
        }
        return tools;
    }

    /**
     * 创建请求头
     */
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(config.getApiKey());
        return headers;
    }

    /**
     * 补充请求参数的默认值
     */
    private void completeRequest(QwenChatRequest request, boolean stream) {
        if (request.getModel() == null) {
            request.setModel(config.getModel());
        }
        request.setStream(stream);

        if (stream) {
            request.setStreamOptions(QwenChatRequest.StreamOptions.builder()
                .includeUsage(true)
                .build());
        }
    }

    /**
     * 记录请求日志
     */
    private void logRequest(QwenChatRequest request) {
        log.info("========== AI 调用开始 ==========");
        log.info("提供商: 通义千问");
        log.info("API 地址: {}", config.getBaseUrl());
        log.info("模型: {}", request.getModel());
        log.info("消息数: {}", request.getMessages() != null ? request.getMessages().size() : 0);
        log.info("流式: {}", request.getStream());

        if (request.getTools() != null && !request.getTools().isEmpty()) {
            log.info("工具数: {}", request.getTools().size());
        }
    }

    /**
     * 记录非流式响应日志
     */
    private void logResponse(ChatResponse response, long duration) {
        log.info("AI 调用成功，耗时: {}ms", duration);

        if (response.getChoices() != null && !response.getChoices().isEmpty()) {
            String content = response.getContent();
            if (content != null) {
                log.info("内容长度: {}", content.length());
            }

            if (response.hasToolCalls()) {
                log.info("工具调用数: {}", response.getToolCallsList().size());
            }

            if (log.isDebugEnabled() && content != null) {
                String preview = content.length() > 100
                    ? content.substring(0, 100) + "..."
                    : content;
                log.debug("回复内容: {}", preview);
            }
        }

        if (response.getUsage() != null) {
            log.info("Token 使用: input={}, output={}, total={}",
                response.getUsage().getPromptTokens(),
                response.getUsage().getCompletionTokens(),
                response.getUsage().getTotalTokens()
            );
        }

        log.info("========== AI 调用结束 ==========");
    }

    /**
     * Builder 类
     */
    public static class Builder {
        private QwenConfig config;
        private RestTemplate restTemplate;
        private ObjectMapper objectMapper;
        private List<AITool> defaultTools = new ArrayList<>();

        public Builder config(QwenConfig config) {
            this.config = config;
            return this;
        }

        public Builder restTemplate(RestTemplate restTemplate) {
            this.restTemplate = restTemplate;
            return this;
        }

        public Builder objectMapper(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
            return this;
        }

        public Builder defaultTools(AITool... tools) {
            this.defaultTools.addAll(Arrays.asList(tools));
            return this;
        }

        public Builder defaultTools(List<AITool> tools) {
            this.defaultTools.addAll(tools);
            return this;
        }

        public QwenChatClient build() {
            // 验证必填字段
            if (config == null) {
                throw new IllegalArgumentException("config 不能为空");
            }
            if (restTemplate == null) {
                throw new IllegalArgumentException("restTemplate 不能为空");
            }
            if (objectMapper == null) {
                throw new IllegalArgumentException("objectMapper 不能为空");
            }

            return new QwenChatClient(this);
        }
    }
}
