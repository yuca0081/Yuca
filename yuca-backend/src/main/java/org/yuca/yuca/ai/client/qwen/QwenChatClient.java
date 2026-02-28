package org.yuca.yuca.ai.client.qwen;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.alibaba.dashscope.common.MultiModalMessage;
import com.alibaba.dashscope.common.Role;
import io.reactivex.Flowable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.yuca.yuca.ai.client.AIChatClient;
import org.yuca.yuca.ai.common.ChatRequest;
import org.yuca.yuca.ai.model.AIMessage;
import org.yuca.yuca.ai.model.ChatResponse;
import org.yuca.yuca.ai.model.MessageRole;
import org.yuca.yuca.ai.model.Usage;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

/**
 * 通义千问 AI 提供商实现
 *
 * @author Yuca
 * @since 2025-01-27
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "qwen.api-key")
public class QwenChatClient implements AIChatClient {

    private final MultiModalConversation multiModalConversation;
    private final QwenConfig config;

    @Override
    public ChatResponse chat(ChatRequest request) {
        long startTime = System.currentTimeMillis();

        try {
            // 1. 转换消息格式
            List<MultiModalMessage> qwenMessages = convertMessages(request.getMessages());

            // 2. 构建请求参数
            MultiModalConversationParam param = buildParam(qwenMessages, request, false);

            // 3. 记录请求日志
            logRequest(request, qwenMessages);

            // 4. 调用 API
            MultiModalConversationResult result = multiModalConversation.call(param);

            // 5. 解析响应
            String modelName = request.getModel() != null ? request.getModel() : config.getModel();
            ChatResponse response = parseResponse(result, modelName);

            // 6. 记录响应日志
            long duration = System.currentTimeMillis() - startTime;
            logResponse(response, duration);

            return response;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("通义千问调用失败，耗时: {}ms", duration, e);
            throw new RuntimeException("AI 调用失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void chatStream(ChatRequest request, Consumer<String> tokenHandler) {
        long startTime = System.currentTimeMillis();

        try {
            // 1. 转换消息格式
            List<MultiModalMessage> qwenMessages = convertMessages(request.getMessages());

            // 2. 构建请求参数
            MultiModalConversationParam param = buildParam(qwenMessages, request, true);

            // 3. 记录请求日志
            logRequest(request, qwenMessages);

            // 4. 流式调用（返回 Flowable）
            Flowable<MultiModalConversationResult> resultFlowable = multiModalConversation.streamCall(param);

            // 5. 使用 CountDownLatch 等待异步完成
            CountDownLatch latch = new CountDownLatch(1);
            final StringBuilder fullContent = new StringBuilder();

            resultFlowable.subscribe(
                // onNext: 处理每个流式结果
                (MultiModalConversationResult result) -> {
                    if (result != null && result.getOutput() != null
                        && result.getOutput().getChoices() != null
                        && !result.getOutput().getChoices().isEmpty()) {

                        // 多模态接口返回的内容是 List<Map<String, Object>> 格式
                        List<?> contentList = result.getOutput().getChoices().get(0).getMessage().getContent();
                        String content = extractTextContent(contentList);

                        if (content != null && !content.isEmpty()) {
                            fullContent.append(content);
                            tokenHandler.accept(content);
                        }
                    }
                },
                // onError: 处理错误
                error -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.error("通义千问流式调用失败，耗时: {}ms", duration, error);
                    latch.countDown();
                },
                // onComplete: 完成时调用
                () -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.info("通义千问流式调用完成，耗时: {}ms, 内容长度: {}", duration, fullContent.length());
                    latch.countDown();
                }
            );

            // 等待流式调用完成
            latch.await();

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("通义千问流式调用失败，耗时: {}ms", duration, e);
            throw new RuntimeException("AI 调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * 转换消息格式
     */
    private List<MultiModalMessage> convertMessages(List<AIMessage> messages) {
        return messages.stream()
            .map(this::createQwenMessage)
            .toList();
    }

    /**
     * 创建通义千问消息
     */
    private MultiModalMessage createQwenMessage(AIMessage msg) {
        Role role = convertRole(msg.getRole());
        return MultiModalMessage.builder()
            .role(role.getValue())
            .content(List.of(
                java.util.Map.of("text", msg.getContent())
            ))
            .build();
    }

    /**
     * 转换角色
     */
    private Role convertRole(MessageRole role) {
        return switch (role) {
            case USER -> Role.USER;
            case ASSISTANT -> Role.ASSISTANT;
            case SYSTEM -> Role.SYSTEM;
            case TOOL -> Role.TOOL;
        };
    }

    /**
     * 构建请求参数
     */
    private MultiModalConversationParam buildParam(List<MultiModalMessage> messages, ChatRequest request, boolean stream) {
        MultiModalConversationParam.MultiModalConversationParamBuilder builder = MultiModalConversationParam.builder()
            .apiKey(config.getApiKey())
            .model(request.getModel() != null ? request.getModel() : config.getModel())
            .messages(messages)
            .enableSearch(request.getEnableSearch() != null ? request.getEnableSearch() : false)
            .maxTokens(request.getMaxTokens() != null ? request.getMaxTokens() : config.getMaxTokens())
            .temperature(request.getTemperature() != null ? request.getTemperature().floatValue() : config.getTemperature());
        if (stream) {
            builder.incrementalOutput(true);
        }

        return builder.build();
    }

    /**
     * 解析响应
     */
    private ChatResponse parseResponse(MultiModalConversationResult result, String modelName) {
        if (result == null || result.getOutput() == null
            || result.getOutput().getChoices() == null
            || result.getOutput().getChoices().isEmpty()) {
            throw new RuntimeException("AI 返回空结果");
        }

        // 多模态接口返回的内容是 List<Map<String, Object>> 格式
        // 需要提取文本内容
        List<?> contentList = result.getOutput().getChoices().get(0).getMessage().getContent();
        String content = extractTextContent(contentList);

        Usage usage = null;
        if (result.getUsage() != null) {
            usage = Usage.builder()
                .inputTokens(result.getUsage().getInputTokens())
                .outputTokens(result.getUsage().getOutputTokens())
                .build();
        }

        return ChatResponse.builder()
            .content(content)
            .usage(usage)
            .requestId(result.getRequestId())
            .model(modelName)
            .build();
    }

    /**
     * 从多模态内容列表中提取文本
     */
    @SuppressWarnings("unchecked")
    private String extractTextContent(List<?> contentList) {
        if (contentList == null || contentList.isEmpty()) {
            return "";
        }

        StringBuilder textBuilder = new StringBuilder();
        for (Object item : contentList) {
            if (item instanceof java.util.Map) {
                Map<String, Object> map = (Map<String, Object>) item;
                Object text = map.get("text");
                if (text != null) {
                    textBuilder.append(text.toString());
                }
            }
        }

        return textBuilder.toString();
    }

    /**
     * 记录请求日志
     */
    private void logRequest(ChatRequest request, List<MultiModalMessage> qwenMessages) {
        log.info("========== AI 调用开始 ==========");
        log.info("模型: {}", request.getModel() != null ? request.getModel() : config.getModel());
        log.info("消息数: {}", qwenMessages.size());

        if (log.isDebugEnabled()) {
            for (int i = 0; i < qwenMessages.size(); i++) {
                MultiModalMessage msg = qwenMessages.get(i);
                List<?> contentList = msg.getContent();
                String textContent = extractTextContent(contentList);
                String preview = textContent.length() > 50
                    ? textContent.substring(0, 50) + "..."
                    : textContent;
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
