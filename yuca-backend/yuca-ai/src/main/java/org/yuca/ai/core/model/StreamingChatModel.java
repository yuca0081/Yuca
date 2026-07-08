package org.yuca.ai.core.model;

/**
 * 流式聊天模型接口。
 * 等价于 langchain4j 的 dev.langchain4j.model.chat.StreamingChatModel。
 */
public interface StreamingChatModel {

    void chat(ChatRequest request, StreamingChatResponseHandler handler);
}
