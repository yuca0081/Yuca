package org.yuca.ai.core.model;

/**
 * 同步聊天模型接口。
 * 等价于 langchain4j 的 dev.langchain4j.model.chat.ChatModel。
 */
public interface ChatModel {

    ChatResponse chat(ChatRequest request);
}
