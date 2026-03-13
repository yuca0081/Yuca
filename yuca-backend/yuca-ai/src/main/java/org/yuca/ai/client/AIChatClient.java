package org.yuca.ai.client;

import org.yuca.ai.model.ChatRequest;
import org.yuca.ai.model.ChatResponse;
import org.yuca.ai.model.AIMessage;

import java.util.List;
import java.util.function.Consumer;

/**
 * AI 聊天客户端接口
 *
 * <p>提供统一的 AI 聊天抽象，支持同步和流式调用
 *
 * @author Yuca
 * @since 2025-01-27
 */
public interface AIChatClient {

    /**
     * 同步聊天（完整请求）
     *
     * @param request 聊天请求
     * @return 聊天响应
     */
    ChatResponse chat(ChatRequest request);

    /**
     * 流式聊天
     *
     * @param request 请求参数
     * @param tokenHandler Token处理器（接收每个token）
     */
    void chatStream(ChatRequest request, Consumer<String> tokenHandler);

    /**
     * 便捷方法：直接聊天（String）
     *
     * @param question 用户问题
     * @return AI 回复
     */
    default String chat(String question) {
        return chat(ChatRequest.builder()
            .messages(List.of(AIMessage.user(question)))
            .build())
            .getContent();
    }

    /**
     * 便捷方法：直接聊天（消息列表）
     *
     * @param messages 消息列表
     * @return AI 回复
     */
    default String chat(List<AIMessage> messages) {
        return chat(ChatRequest.builder()
            .messages(messages)
            .build())
            .getContent();
    }
}
