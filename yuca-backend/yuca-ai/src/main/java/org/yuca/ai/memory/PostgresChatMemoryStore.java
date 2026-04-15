package org.yuca.ai.memory;

import com.google.gson.Gson;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.yuca.ai.entity.Conversation;
import org.yuca.ai.mapper.ConversationMapper;

import java.time.LocalDateTime;
import java.util.List;

/**
 * PostgreSQL 聊天记忆存储
 * 实现 ChatMemoryStore 接口
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PostgresChatMemoryStore implements ChatMemoryStore {

    private final ConversationMapper conversationMapper;
    private final Gson gson;

    @Override
    public List<ChatMessage> getMessages(String sessionId) {
        try {
            List<Conversation> conversations = conversationMapper.selectBySessionId(sessionId);
            log.debug("从 PostgreSQL 加载 {} 条消息, sessionId={}", conversations.size(), sessionId);
            return conversations.stream()
                    .map(this::toChatMessage)
                    .toList();
        } catch (Exception e) {
            log.error("加载消息失败, sessionId={}", sessionId, e);
            return List.of();
        }
    }

    @Override
    public void appendMessages(String sessionId, List<ChatMessage> messages) {
        for (ChatMessage message : messages) {
            try {
                saveMessage(sessionId, message);
            } catch (Exception e) {
                log.error("保存消息失败, sessionId={}, type={}", sessionId, message.type(), e);
            }
        }
        log.debug("追加 {} 条消息, sessionId={}", messages.size(), sessionId);
    }

    @Override
    public void deleteMessages(String sessionId) {
        try {
            conversationMapper.deleteBySessionId(sessionId);
            log.info("删除会话消息, sessionId={}", sessionId);
        } catch (Exception e) {
            log.error("删除消息失败, sessionId={}", sessionId, e);
        }
    }

    private void saveMessage(String sessionId, ChatMessage message) {
        Conversation conversation = new Conversation();
        conversation.setSessionId(sessionId);
        conversation.setCreatedAt(LocalDateTime.now());

        switch (message) {
            case UserMessage userMsg -> {
                conversation.setMessageType("USER");
                conversation.setContent(userMsg.singleText());
            }
            case SystemMessage sysMsg -> {
                conversation.setMessageType("SYSTEM");
                conversation.setContent(sysMsg.text());
            }
            case AiMessage aiMsg when aiMsg.hasToolExecutionRequests() -> {
                conversation.setMessageType("TOOL");
                conversation.setContent(aiMsg.text() != null ? aiMsg.text() : "");
                conversation.setToolCalls(gson.toJson(aiMsg.toolExecutionRequests()));
            }
            case AiMessage aiMsg -> {
                conversation.setMessageType("AI");
                conversation.setContent(aiMsg.text());
            }
            case ToolExecutionResultMessage resultMsg -> {
                // 工具执行结果暂不保存
                return;
            }
            default -> {
                log.warn("未知消息类型: {}", message.type());
                return;
            }
        }

        conversationMapper.insert(conversation);
    }

    private ChatMessage toChatMessage(Conversation conversation) {
        return switch (conversation.getMessageType()) {
            case "USER" -> UserMessage.from(conversation.getContent());
            case "SYSTEM" -> SystemMessage.from(conversation.getContent());
            case "AI", "TOOL" -> AiMessage.from(conversation.getContent());
            default -> throw new IllegalArgumentException("未知消息类型: " + conversation.getMessageType());
        };
    }
}
