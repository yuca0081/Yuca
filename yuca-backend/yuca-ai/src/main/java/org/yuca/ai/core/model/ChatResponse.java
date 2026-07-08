package org.yuca.ai.core.model;

import org.yuca.ai.core.message.AiMessage;

/**
 * 聊天响应。
 * 等价于 langchain4j 的 dev.langchain4j.model.chat.response.ChatResponse。
 */
public record ChatResponse(AiMessage aiMessage, TokenUsage tokenUsage) {

    public static ChatResponse of(AiMessage aiMessage) {
        return new ChatResponse(aiMessage, null);
    }
}
