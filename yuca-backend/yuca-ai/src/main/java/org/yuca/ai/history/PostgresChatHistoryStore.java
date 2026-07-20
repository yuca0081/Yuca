package org.yuca.ai.history;

import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import com.google.gson.reflect.TypeToken;
import org.yuca.ai.core.message.AiMessage;
import org.yuca.ai.core.message.ChatMessage;
import org.yuca.ai.core.message.SystemMessage;
import org.yuca.ai.core.message.ToolExecutionResultMessage;
import org.yuca.ai.core.message.UserMessage;
import org.yuca.ai.core.tool.ToolExecutionRequest;
import org.yuca.ai.mapper.ChatHistoryMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * PostgreSQL 对话历史存储
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PostgresChatHistoryStore implements ChatHistoryStore {

    private final ChatHistoryMapper chatHistoryMapper;
    private final Gson gson;

    @Override
    public List<ChatMessage> getMessages(String sessionId) {
        try {
            List<ChatHistory> histories = chatHistoryMapper.selectBySessionId(sessionId);
            log.debug("从 PostgreSQL 加载 {} 条消息, sessionId={}", histories.size(), sessionId);
            return histories.stream()
                    .map(this::toChatMessage)
                    .toList();
        } catch (Exception e) {
            log.error("加载消息失败, sessionId={}", sessionId, e);
            return List.of();
        }
    }

    @Override
    public List<ChatMessage> getActiveMessages(String sessionId) {
        try {
            List<ChatHistory> histories = chatHistoryMapper.selectBySessionId(sessionId);
            if (histories.isEmpty()) {
                return List.of();
            }

            // 从尾向头找最新一条 SUMMARY——它就是当前活跃历史的起点
            // SUMMARY 之前的所有消息已被该摘要取代，不再加载
            int summaryIdx = -1;
            for (int i = histories.size() - 1; i >= 0; i--) {
                if ("SUMMARY".equals(histories.get(i).getMessageType())) {
                    summaryIdx = i;
                    break;
                }
            }

            List<ChatHistory> active = summaryIdx >= 0
                    ? histories.subList(summaryIdx, histories.size())
                    : histories;

            log.debug("加载 active 消息: sessionId={}, total={}, active={}, summaryAt={}",
                    sessionId, histories.size(), active.size(), summaryIdx);
            return active.stream()
                    .map(this::toChatMessage)
                    .toList();
        } catch (Exception e) {
            log.error("加载 active 消息失败, sessionId={}", sessionId, e);
            return List.of();
        }
    }

    @Override
    public void appendMessages(String sessionId, List<ChatHistory> messages) {
        for (ChatHistory history : messages) {
            try {
                chatHistoryMapper.insert(history);
            } catch (Exception e) {
                log.error("保存消息失败, sessionId={}, type={}", sessionId, history.getMessageType(), e);
            }
        }
        log.debug("追加 {} 条消息, sessionId={}", messages.size(), sessionId);
    }

    @Override
    public void deleteMessages(String sessionId) {
        try {
            chatHistoryMapper.deleteBySessionId(sessionId);
            log.info("删除会话消息, sessionId={}", sessionId);
        } catch (Exception e) {
            log.error("删除消息失败, sessionId={}", sessionId, e);
        }
    }

    /**
     * 将 ChatMessage 转换为 ChatHistory 实体
     * 供 MemoryEnhancer 调用
     */
    public static ChatHistory toChatHistory(String sessionId, ChatMessage message) {
        ChatHistory history = new ChatHistory();
        history.setSessionId(sessionId);
        history.setCreatedAt(LocalDateTime.now());

        switch (message) {
            case UserMessage userMsg -> {
                history.setMessageType("USER");
                history.setContent(userMsg.singleText());
            }
            case SystemMessage sysMsg -> {
                history.setMessageType("SYSTEM");
                history.setContent(sysMsg.text());
            }
            case AiMessage aiMsg when aiMsg.hasToolExecutionRequests() -> {
                history.setMessageType("TOOL");
                history.setContent(aiMsg.text() != null ? aiMsg.text() : "");
            }
            case AiMessage aiMsg -> {
                history.setMessageType("AI");
                history.setContent(aiMsg.text());
            }
            case ToolExecutionResultMessage resultMsg -> {
                history.setMessageType("TOOL_RESULT");
                history.setContent(resultMsg.text());
            }
            default -> throw new IllegalArgumentException("未知消息类型: " + message.type());
        }

        return history;
    }

    /**
     * 将 ChatMessage 转换为 ChatHistory，并附带 toolCalls 和 tokenUsage
     */
    public static ChatHistory toChatHistory(String sessionId, ChatMessage message, Gson gson) {
        ChatHistory history = toChatHistory(sessionId, message);

        // 工具调用信息
        if (message instanceof AiMessage aiMsg && aiMsg.hasToolExecutionRequests()) {
            history.setToolCalls(gson.toJson(aiMsg.toolExecutionRequests()));
        } else if (message instanceof ToolExecutionResultMessage resultMsg) {
            history.setToolCalls(gson.toJson(Map.of(
                    "id", resultMsg.id() != null ? resultMsg.id() : "",
                    "toolName", resultMsg.toolName() != null ? resultMsg.toolName() : ""
            )));
        }

        return history;
    }

    private ChatMessage toChatMessage(ChatHistory history) {
        return switch (history.getMessageType()) {
            case "USER" -> UserMessage.from(history.getContent());
            case "SYSTEM" -> SystemMessage.from(history.getContent());
            // SUMMARY 也映射为 SystemMessage：它是系统注入的"前期对话摘要"上下文，
            // 不算真正的 user/ai 轮次，HistoryEnhancer.after 也会据此把它排除在保存集合外
            case "SUMMARY" -> SystemMessage.from("[前期对话摘要] " + history.getContent());
            case "AI" -> AiMessage.from(history.getContent());
            case "TOOL" -> {
                List<ToolExecutionRequest> requests = gson.fromJson(
                        history.getToolCalls(), new TypeToken<List<ToolExecutionRequest>>() {}.getType());
                String text = history.getContent();
                yield (text != null && !text.isEmpty())
                        ? AiMessage.from(text, requests) : AiMessage.from(requests);
            }
            case "TOOL_RESULT" -> {
                Map<String, String> toolInfo = gson.fromJson(
                        history.getToolCalls(), new TypeToken<Map<String, String>>() {}.getType());
                ToolExecutionRequest request = ToolExecutionRequest.builder()
                        .id(toolInfo.getOrDefault("id", ""))
                        .name(toolInfo.getOrDefault("toolName", ""))
                        .build();
                yield ToolExecutionResultMessage.from(request, history.getContent());
            }
            default -> throw new IllegalArgumentException("未知消息类型: " + history.getMessageType());
        };
    }
}
