package org.yuca.ai.history;

import org.yuca.ai.core.message.ChatMessage;

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
     * 获取指定会话的"活跃"消息——从最新一条 SUMMARY（含）到末尾。
     *
     * <p>用于 HistoryEnhancer 加载历史上下文：SUMMARY 之前的消息已被摘要取代，
     * 再加载纯属浪费 token。无 SUMMARY 时返回全量（等价于 {@link #getMessages}）。
     */
    List<ChatMessage> getActiveMessages(String sessionId);

    /**
     * 追加消息到指定会话
     */
    void appendMessages(String sessionId, List<ChatHistory> messages);

    /**
     * 删除指定会话的所有消息
     */
    void deleteMessages(String sessionId);
}
