package org.yuca.ai.agent.enhancer;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.extern.slf4j.Slf4j;
import org.yuca.ai.agent.ChatContext;
import org.yuca.ai.agent.ChatEnhancer;
import org.yuca.ai.memory.ChatMemoryStore;

import java.util.ArrayList;
import java.util.List;

/**
 * 记忆增强器
 * before: 从 store 加载历史消息
 * after: 保存本次 user + AI 消息到 store
 */
@Slf4j
public class MemoryEnhancer implements ChatEnhancer {

    private final ChatMemoryStore memoryStore;
    private final int maxMessages;

    // 缓存当前请求的用户消息，供 after 使用
    private static final String USER_MESSAGES_KEY = "_userMessages";
    private static final String HISTORY_COUNT_KEY = "_historyCount";
    private static final String AGENT_CONVERSATION_KEY = "_agentConversation";

    public MemoryEnhancer(ChatMemoryStore memoryStore, int maxMessages) {
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
        // 记录历史消息数量，after 时用于跳过已存储的历史
        context.attribute(HISTORY_COUNT_KEY, trimmed.size());

        // 拼装：历史 + 当前请求
        List<ChatMessage> messages = new ArrayList<>(trimmed);
        messages.addAll(request.messages());

        log.debug("MemoryEnhancer.before: sessionId={}, history={}, total={}",
                sessionId, history.size(), messages.size());

        return ChatRequest.builder().messages(messages).build();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void after(ChatResponse response, ChatContext context) {
        String sessionId = context.getSessionId();
        if (sessionId == null) {
            return;
        }

        List<ChatMessage> toSave;

        // 优先使用 Agent 提供的完整对话（包含工具调用中间过程）
        List<ChatMessage> agentConversation = context.attribute(AGENT_CONVERSATION_KEY);
        Integer historyCount = context.attribute(HISTORY_COUNT_KEY);

        if (agentConversation != null && historyCount != null) {
            // 跳过历史消息，过滤 SystemMessage
            toSave = agentConversation.subList(historyCount, agentConversation.size())
                    .stream()
                    .filter(msg -> !(msg instanceof SystemMessage))
                    .toList();
        } else {
            // 兜底：只保存用户消息 + 最终响应
            if (response == null || response.aiMessage() == null) {
                return;
            }
            List<ChatMessage> userMessages = context.attribute(USER_MESSAGES_KEY);
            if (userMessages == null) {
                userMessages = List.of();
            }
            toSave = new ArrayList<>(userMessages.stream()
                    .filter(msg -> !(msg instanceof SystemMessage))
                    .toList());
            toSave.add(response.aiMessage());
        }

        if (!toSave.isEmpty()) {
            memoryStore.appendMessages(sessionId, toSave);
            log.debug("MemoryEnhancer.after: sessionId={}, saved={}", sessionId, toSave.size());
        }
    }
}
