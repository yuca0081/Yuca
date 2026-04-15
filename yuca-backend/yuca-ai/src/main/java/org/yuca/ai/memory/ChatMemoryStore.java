package org.yuca.ai.memory;

import dev.langchain4j.data.message.ChatMessage;

import java.util.List;

/**
 * 聊天记忆存储接口
 */
public interface ChatMemoryStore {

    /**
     * 获取指定会话的所有消息
     */
    List<ChatMessage> getMessages(String sessionId);

    /**
     * 追加消息到指定会话
     */
    void appendMessages(String sessionId, List<ChatMessage> messages);

    /**
     * 删除指定会话的所有消息
     */
    void deleteMessages(String sessionId);
}
