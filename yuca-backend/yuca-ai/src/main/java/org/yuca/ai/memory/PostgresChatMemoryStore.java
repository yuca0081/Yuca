package org.yuca.ai.memory;

import com.google.gson.Gson;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.*;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.yuca.ai.entity.Conversation;
import org.yuca.ai.mapper.ConversationMapper;

import java.time.LocalDateTime;
import java.util.List;

/**
 * PostgreSQL ChatMemoryStore 简化版
 * 直接实现ChatMemoryStore接口，用于AiService装配
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PostgresChatMemoryStore implements ChatMemoryStore {

    private final ConversationMapper conversationMapper;
    private final Gson gson;

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        String sessionId = memoryId.toString();
        try {
            List<Conversation> conversations = conversationMapper.selectBySessionId(sessionId);
            log.debug("从PostgreSQL加载 {} 条消息，sessionId: {}", conversations.size(), sessionId);
            return conversations.stream()
                    .map(this::toChatMessage)
                    .toList();
        } catch (Exception e) {
            log.error("加载消息失败，sessionId: {}", sessionId, e);
            return List.of();
        }
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        String sessionId = memoryId.toString();
        try {
            // 获取数据库中现有的消息数
            List<Conversation> existingMessages = conversationMapper.selectBySessionId(sessionId);
            int existingCount = existingMessages.size();

            // 只保存新增的消息
            if (messages.size() > existingCount) {
                List<ChatMessage> newMessages = messages.subList(existingCount, messages.size());

                int successCount = 0;
                int failCount = 0;

                for (ChatMessage message : newMessages) {
                    try {
                        saveMessage(sessionId, message);
                        successCount++;
                    } catch (Exception e) {
                        // 单条消息保存失败不影响其他消息
                        log.error("保存单条消息失败，sessionId: {}, type: {}", sessionId, message.type(), e);
                        failCount++;
                    }
                }

                log.info("保存消息到PostgreSQL完成，sessionId: {}, 成功: {}, 失败: {}",
                        sessionId, successCount, failCount);
            }

        } catch (Exception e) {
            log.error("保存消息失败，sessionId: {}", sessionId, e);
        }
    }

    @Override
    public void deleteMessages(Object memoryId) {
        String sessionId = memoryId.toString();
        try {
            conversationMapper.deleteBySessionId(sessionId);
            log.info("删除会话消息，sessionId: {}", sessionId);
        } catch (Exception e) {
            log.error("删除消息失败，sessionId: {}", sessionId, e);
        }
    }

    /**
     * 保存单条消息到数据库
     * 支持多种消息类型：USER, SYSTEM, AI, TOOL
     */
    private void saveMessage(String sessionId, ChatMessage message) {
        Conversation conversation = new Conversation();
        conversation.setSessionId(sessionId);
        conversation.setCreatedAt(LocalDateTime.now());

        try {
            // 根据消息类型设置内容
            if (message instanceof UserMessage userMessage) {
                // 用户消息
                conversation.setMessageType("USER");
                conversation.setContent(userMessage.singleText());

            } else if (message instanceof SystemMessage systemMessage) {
                // 系统消息（如提示词）
                conversation.setMessageType("SYSTEM");
                conversation.setContent(systemMessage.text());
            } else if (message instanceof AiMessage aiMessage) {
                handleAiMessage(conversation, aiMessage);
            } else if (message instanceof ToolExecutionResultMessage resultMessage) {
                // 工具执行结果消息，跳过保存
                conversation.setContent(resultMessage.text());
                conversation.setMessageType("TOOL_RESULT");
                log.debug("跳过工具执行结果消息");
                return;
            } else {
                // 未知消息类型
                log.warn("未知的消息类型: {}, 跳过保存", message.type());
                return;
            }

            // 插入数据库
            conversationMapper.insert(conversation);
            log.debug("保存消息成功: sessionId={}, type={}, content={}",
                    sessionId, conversation.getMessageType(),
                    conversation.getContent() != null ?
                        conversation.getContent().substring(0, Math.min(50, conversation.getContent().length())) : "null");

        } catch (Exception e) {
            // 保存失败时，尝试不保存 tool_calls，只保存消息内容
            log.warn("保存消息失败（可能包含 tool_calls），尝试不保存 tool_calls: {}", e.getMessage());

            try {
                conversation.setToolCalls(null);  // 清空 tool_calls
                conversationMapper.insert(conversation);
                log.info("保存消息成功（不包含 tool_calls）: sessionId={}, type={}",
                        sessionId, conversation.getMessageType());
            } catch (Exception ex) {
                log.error("保存消息彻底失败，跳过此消息: sessionId={}, type={}",
                        sessionId, conversation.getMessageType(), ex);
            }
        }
    }

    /**
     * 处理AI消息（包括普通回复和工具调用）
     */
    private void handleAiMessage(Conversation conversation, AiMessage aiMessage) {
        if (aiMessage.hasToolExecutionRequests()) {
            // AI调用工具的消息
            conversation.setMessageType("TOOL");

            // 尝试获取文本内容
            String textContent = getTextSafely(aiMessage);
            conversation.setContent(textContent);

            // 保存工具调用信息
            saveToolCalls(conversation, aiMessage);
        } else {
            // 普通AI回复消息
            conversation.setMessageType("AI");
            String text = aiMessage.text();
            conversation.setContent(text != null ? text : "[AI回复]");
        }
    }

    /**
     * 安全地获取AI消息的文本内容
     * 处理包含工具调用的情况
     */
    private String getTextSafely(AiMessage aiMessage) {
        try {
            String text = aiMessage.text();
            if (text != null && !text.isEmpty()) {
                return text;
            }

            // 如果text为空，可能是工具调用消息
            if (aiMessage.hasToolExecutionRequests()) {
                ToolExecutionRequest request = aiMessage.toolExecutionRequests().get(0);
                return "[调用工具: " + request.name() + " 参数: "
                        + request.arguments().toString() + "]";
            }

            return "[AI消息]";
        } catch (Exception e) {
            log.error("获取AI消息文本失败", e);
            return "[AI消息]";
        }
    }

    /**
     * 保存工具调用信息
     */
    private void saveToolCalls(Conversation conversation, AiMessage aiMessage) {
        try {
            List<ToolExecutionRequest> toolRequests = aiMessage.toolExecutionRequests();

            String toolCallsJson = gson.toJson(toolRequests);
            conversation.setToolCalls(toolCallsJson);

            log.debug("保存工具调用信息: tool count={}", toolRequests.size());

        } catch (Exception e) {
            log.warn("无法序列化工具调用信息，将跳过保存: {}", e.getMessage());
            conversation.setToolCalls(null);
        }
    }

    /**
     * 将数据库记录转换为ChatMessage
     */
    private ChatMessage toChatMessage(Conversation conversation) {
        return switch (conversation.getMessageType()) {
            case "USER" -> UserMessage.from(conversation.getContent());
            case "AI" -> AiMessage.from(conversation.getContent());
            case "SYSTEM" -> SystemMessage.from(conversation.getContent());
            case "TOOL" -> AiMessage.from(conversation.getContent());
            default -> throw new IllegalArgumentException("未知消息类型: " + conversation.getMessageType());
        };
    }
}
