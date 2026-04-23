package org.yuca.ai.agent.enhancer;

import com.google.gson.Gson;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.extern.slf4j.Slf4j;
import org.yuca.ai.agent.ChatContext;
import org.yuca.ai.history.ChatHistory;
import org.yuca.ai.history.ChatHistoryStore;
import org.yuca.ai.history.PostgresChatHistoryStore;

import java.util.ArrayList;
import java.util.List;

/**
 * 对话历史增强器
 * before: 从 store 加载历史消息
 * after: 保存本次 user + AI 消息到 store
 */
@Slf4j
public class HistoryEnhancer implements ChatEnhancer {

    private final ChatHistoryStore historyStore;
    private final int maxMessages;
    private final Gson gson = new Gson();

    private static final String USER_MESSAGES_KEY = "_userMessages";
    private static final String HISTORY_COUNT_KEY = "_historyCount";
    private static final String AGENT_CONVERSATION_KEY = "_agentConversation";

    public HistoryEnhancer(ChatHistoryStore historyStore, int maxMessages) {
        this.historyStore = historyStore;
        this.maxMessages = maxMessages;
    }

    @Override
    public ChatRequest before(ChatRequest request, ChatContext context) {
        String sessionId = context.getSessionId();
        if (sessionId == null) {
            return request;
        }

        List<ChatMessage> history = historyStore.getMessages(sessionId);
        List<ChatMessage> trimmed = history.size() > maxMessages
                ? history.subList(history.size() - maxMessages, history.size())
                : history;

        context.attribute(USER_MESSAGES_KEY, request.messages());
        context.attribute(HISTORY_COUNT_KEY, trimmed.size());

        List<ChatMessage> messages = new ArrayList<>(trimmed);
        messages.addAll(request.messages());

        log.debug("HistoryEnhancer.before: sessionId={}, history={}, total={}",
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

        List<ChatMessage> agentConversation = context.attribute(AGENT_CONVERSATION_KEY);
        Integer historyCount = context.attribute(HISTORY_COUNT_KEY);

        if (agentConversation != null && historyCount != null) {
            toSave = agentConversation.subList(historyCount, agentConversation.size())
                    .stream()
                    .filter(msg -> !(msg instanceof SystemMessage))
                    .toList();
        } else {
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
            // 转换为 ChatHistory 实体
            List<ChatHistory> histories = new ArrayList<>();
            for (ChatMessage msg : toSave) {
                ChatHistory history = PostgresChatHistoryStore.toChatHistory(sessionId, msg, gson);

                // AI 消息附带 tokenUsage
                if (msg instanceof AiMessage && response != null && response.tokenUsage() != null) {
                    history.setTokenUsage(gson.toJson(response.tokenUsage()));
                }

                histories.add(history);
            }

            historyStore.appendMessages(sessionId, histories);
            log.debug("HistoryEnhancer.after: sessionId={}, saved={}", sessionId, histories.size());
        }
    }
}
