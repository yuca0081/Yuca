package org.yuca.ai.history;

import dev.langchain4j.data.message.ChatMessage;

import java.util.List;

/**
 * 对话历史存储接口
 */
public interface ChatHistoryStore {

    /**
     * 获取指定会话的所有消息
     */
    List<ChatMessage> getMessages(String sessionId);

    /**
     * 追加消息到指定会话
     */
    void appendMessages(String sessionId, List<ChatHistory> messages);

    /**
     * 删除指定会话的所有消息
     */
    void deleteMessages(String sessionId);
}
