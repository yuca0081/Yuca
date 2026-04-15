package org.yuca.ai.agent.interceptor;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.extern.slf4j.Slf4j;
import org.yuca.ai.agent.ChatContext;
import org.yuca.ai.agent.ChatInterceptor;
import org.yuca.ai.memory.ChatMemoryStore;

import java.util.ArrayList;
import java.util.List;

/**
 * 记忆拦截器
 * before: 从 store 加载历史消息
 * after: 保存本次 user + AI 消息到 store
 */
@Slf4j
public class MemoryInterceptor implements ChatInterceptor {

    private final ChatMemoryStore memoryStore;
    private final int maxMessages;

    // 缓存当前请求的用户消息，供 after 使用
    private static final String USER_MESSAGES_KEY = "_userMessages";

    public MemoryInterceptor(ChatMemoryStore memoryStore, int maxMessages) {
        this.memoryStore = memoryStore;
        this.maxMessages = maxMessages;
    }

    @Override
    public ChatRequest before(ChatRequest request, ChatContext context) {
        String sessionId = context.getSessionId();
        if (sessionId == null) {
            return request;
        }

        // 从 store 加载历史
        List<ChatMessage> history = memoryStore.getMessages(sessionId);
        List<ChatMessage> trimmed = history.size() > maxMessages
                ? history.subList(history.size() - maxMessages, history.size())
                : history;

        // 缓存用户消息供 after 使用
        context.attribute(USER_MESSAGES_KEY, request.messages());

        // 拼装：历史 + 当前请求
        List<ChatMessage> messages = new ArrayList<>(trimmed);
        messages.addAll(request.messages());

        log.debug("MemoryInterceptor.before: sessionId={}, history={}, total={}",
                sessionId, history.size(), messages.size());

        return ChatRequest.builder().messages(messages).build();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void after(ChatResponse response, ChatContext context) {
        String sessionId = context.getSessionId();
        if (sessionId == null || response == null || response.aiMessage() == null) {
            return;
        }

        List<ChatMessage> userMessages = context.attribute(USER_MESSAGES_KEY);
        if (userMessages == null) {
            userMessages = List.of();
        }

        // 保存用户消息 + AI 响应
        List<ChatMessage> toSave = new ArrayList<>(userMessages);
        toSave.add(response.aiMessage());

        memoryStore.appendMessages(sessionId, toSave);
        log.debug("MemoryInterceptor.after: sessionId={}, saved={}", sessionId, toSave.size());
    }
}
