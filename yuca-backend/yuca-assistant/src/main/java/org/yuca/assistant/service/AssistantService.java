package org.yuca.assistant.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.yuca.ai.client.qwen.QwenChatClient;
import org.yuca.ai.client.qwen.QwenConfig;
import org.yuca.ai.client.qwen.dto.QwenChatRequest;
import org.yuca.ai.model.*;
import org.yuca.assistant.dto.request.AssistantChatRequest;
import org.yuca.assistant.dto.response.MessageDTO;
import org.yuca.assistant.dto.response.SessionDTO;
import org.yuca.assistant.entity.AssistantMessage;
import org.yuca.assistant.mapper.AssistantSessionMapper;
import org.yuca.ai.tool.AIToolRegistry;
import org.yuca.assistant.dto.sse.SseEvent;
import org.yuca.assistant.entity.AssistantSession;
import org.yuca.assistant.mapper.AssistantMessageMapper;
import org.yuca.common.exception.BusinessException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * AI助手业务服务
 *
 * @author Yuca
 * @since 2025-01-27
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AssistantService {

    private final QwenConfig qwenConfig;
    private final RestTemplate qwenRestTemplate;
    private final ObjectMapper objectMapper;
    private final AIToolRegistry toolRegistry;
    private final AssistantMessageMapper messageMapper;
    private final AssistantSessionMapper sessionMapper;

    @Value("${assistant.max-context-messages:20}")
    private Integer maxContextMessages;

    /**
     * 处理聊天请求（SSE流式）
     */
    @Transactional
    public void processChat(Long userId, AssistantChatRequest request, SseEmitter emitter, HttpServletResponse response) {
        long startTime = System.currentTimeMillis();
        log.info("开始处理聊天请求 - 用户ID: {}, 会话ID: {}", userId, request.getSessionId());

        try {
            // 立即发送一个SSE注释，强制flush响应缓冲，确保连接保持打开
            try {
                emitter.send(SseEmitter.event().comment("keep-alive"));
            } catch (IOException e) {
                log.error("发送keep-alive注释失败", e);
            }

            // 1. 验证会话归属
            AssistantSession session = validateSessionOwnership(request.getSessionId(), userId);

            // 2. 保存用户消息
            AssistantMessage userMessage = saveUserMessage(request.getSessionId(), request.getContent());

            // 3. 发送开始事件
            sendSseEvent(emitter, response, new SseEvent.SseStartEvent(userMessage.getId()));

            // 4. 加载上下文
            List<AssistantMessage> historyMessages = loadContextMessages(request.getSessionId());
            List<QwenChatRequest.QwenMessage> qwenMessages = buildQwenMessages(historyMessages, request.getContent());

            // 5. 创建千问客户端（带默认工具）
            QwenChatClient qwenChatClient = QwenChatClient.builder()
                .config(qwenConfig)
                .restTemplate(qwenRestTemplate)
                .objectMapper(objectMapper)
                .defaultTools(toolRegistry.getAllTools())
                .build();

            // 6. 构建请求
            QwenChatRequest.QwenChatRequestBuilder requestBuilder = QwenChatRequest.builder()
                .messages(qwenMessages);

            // 设置深度思考模式
            if (request.getEnableThinking() != null && request.getEnableThinking()) {
                requestBuilder.enableThinking(true);
            }

            // 设置联网搜索
            if (request.getEnableSearch() != null && request.getEnableSearch()) {
                requestBuilder.enableSearch(true);
            }

            QwenChatRequest qwenRequest = requestBuilder.build();

            StringBuilder fullResponse = new StringBuilder();
            StringBuilder thinkingResponse = new StringBuilder();  // 收集思考内容
            long tokenStartTime = System.currentTimeMillis();
            AtomicInteger tokenCount = new AtomicInteger(0);

            // 使用回调方式处理流式响应（带类型信息和usage统计）
            ChatStreamResponse aiResponse = qwenChatClient.chatStream(qwenRequest, token -> {
                int currentTokenNum = tokenCount.incrementAndGet();
                long tokenTime = System.currentTimeMillis() - tokenStartTime;
                log.info("发送token #{}: type={}, content=\"{}\" (AI返回后{}ms)",
                    currentTokenNum, token.getType(), token.getContent(), tokenTime);

                // 根据token类型发送不同的事件
                if ("thinking".equals(token.getType())) {
                    // 思考内容
                    thinkingResponse.append(token.getContent());
                    sendSseEvent(emitter, response, new SseEvent.SseThinkingEvent(token.getContent()));
                } else {
                    // 正式回答
                    fullResponse.append(token.getContent());
                    sendSseEvent(emitter, response, new SseEvent.SseTokenEvent(token.getContent()));
                }
            });

            // 准备token使用数据
            Integer inputTokens = null;
            Integer outputTokens = null;
            String promptTokensDetails = null;
            Integer totalTokens = null;

            if (aiResponse != null && aiResponse.getUsage() != null) {
                inputTokens = aiResponse.getUsage().getInputTokens();
                outputTokens = aiResponse.getUsage().getOutputTokens();
                totalTokens = aiResponse.getUsage().getTotalTokens();

                // 序列化 promptTokensDetails 为 JSON 字符串
                if (aiResponse.getUsage().getPromptTokensDetails() != null) {
                    try {
                        promptTokensDetails = objectMapper.writeValueAsString(aiResponse.getUsage().getPromptTokensDetails());
                    } catch (Exception e) {
                        log.warn("序列化 promptTokensDetails 失败", e);
                    }
                }

                log.info("Token使用统计 - input: {}, output: {}, total: {}",
                    inputTokens, outputTokens, totalTokens);
            }

            // 流式响应完成后，保存AI消息（包含思考内容和token统计）
            AssistantMessage assistantMessage = saveAssistantMessage(
                request.getSessionId(),
                fullResponse.toString(),
                !thinkingResponse.isEmpty() ? thinkingResponse.toString() : null,
                inputTokens,
                outputTokens,
                promptTokensDetails,
                totalTokens
            );

            // 更新会话时间
            updateSessionTime(session);

            // 发送完成事件（包含token统计）
            sendSseEvent(emitter, response, new SseEvent.SseDoneEvent(
                assistantMessage.getId(),
                fullResponse.toString(),
                inputTokens,
                outputTokens,
                totalTokens
            ));
            emitter.complete();

            // 生成标题（首次对话）
            if (session.getTitle() == null || session.getTitle().isEmpty()) {
                generateTitle(session, request.getContent());
            }

            long totalDuration = System.currentTimeMillis() - startTime;
            log.info("聊天请求处理完成，总耗时: {}ms", totalDuration);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("处理聊天失败，耗时: {}ms", duration, e);
            sendSseEvent(emitter, response, new SseEvent.SseErrorEvent(e.getMessage()));
            emitter.completeWithError(e);
        }
    }

    /**
     * 获取用户会话列表
     */
    public List<SessionDTO> getSessions(Long userId, Integer offset, Integer limit) {
        LambdaQueryWrapper<AssistantSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AssistantSession::getUserId, userId)
            .orderByDesc(AssistantSession::getUpdatedAt)
            .last("LIMIT " + limit + " OFFSET " + offset);

        List<AssistantSession> sessions = sessionMapper.selectList(wrapper);

        return sessions.stream().map(session -> {
            String lastMessage = getLastMessagePreview(session.getId());
            return SessionDTO.builder()
                .id(session.getId())
                .title(session.getTitle())
                .modelName(session.getModelName())
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .lastMessagePreview(lastMessage)
                .build();
        }).toList();
    }

    /**
     * 创建新会话
     */
    @Transactional
    public SessionDTO createSession(Long userId, String modelName) {
        AssistantSession session = AssistantSession.builder()
            .userId(userId)
            .modelName(modelName != null ? modelName : "qwen-plus")
            .title(null)
            .build();

        sessionMapper.insert(session);

        return SessionDTO.builder()
            .id(session.getId())
            .title(session.getTitle())
            .modelName(session.getModelName())
            .createdAt(session.getCreatedAt())
            .updatedAt(session.getUpdatedAt())
            .build();
    }

    /**
     * 删除会话（软删除）
     */
    @Transactional
    public void deleteSession(Long sessionId, Long userId) {
        validateSessionOwnership(sessionId, userId);
        sessionMapper.deleteById(sessionId);
    }

    /**
     * 获取会话详情（含消息）
     */
    public SessionDTO getSessionDetail(Long sessionId, Long userId) {
        AssistantSession session = validateSessionOwnership(sessionId, userId);

        LambdaQueryWrapper<AssistantMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AssistantMessage::getSessionId, sessionId)
            .orderByAsc(AssistantMessage::getCreatedAt);

        List<AssistantMessage> messages = messageMapper.selectList(wrapper);
        List<MessageDTO> messageDTOs = messages.stream()
            .map(msg -> MessageDTO.builder()
                .id(msg.getId())
                .role(msg.getRole())
                .content(msg.getContent())
                .thinkingContent(msg.getThinkingContent())
                .inputTokens(msg.getInputTokens())
                .outputTokens(msg.getOutputTokens())
                .promptTokensDetails(msg.getPromptTokensDetails())
                .totalTokens(msg.getTotalTokens())
                .createdAt(msg.getCreatedAt())
                .build())
            .toList();

        return SessionDTO.builder()
            .id(session.getId())
            .title(session.getTitle())
            .modelName(session.getModelName())
            .createdAt(session.getCreatedAt())
            .updatedAt(session.getUpdatedAt())
            .messages(messageDTOs)
            .build();
    }

    // ==================== 私有辅助方法 ====================

    private AssistantMessage saveUserMessage(Long sessionId, String content) {
        AssistantMessage message = AssistantMessage.builder()
            .sessionId(sessionId)
            .role(MessageRole.USER.getRole())
            .content(content)
            .createdAt(LocalDateTime.now())
            .build();
        messageMapper.insert(message);
        return message;
    }

    private AssistantMessage saveAssistantMessage(Long sessionId, String content, String thinkingContent,
                                                  Integer inputTokens, Integer outputTokens,
                                                  String promptTokensDetails, Integer totalTokens) {
        AssistantMessage message = AssistantMessage.builder()
            .sessionId(sessionId)
            .role(MessageRole.ASSISTANT.getRole())
            .content(content)
            .thinkingContent(thinkingContent)
            .inputTokens(inputTokens)
            .outputTokens(outputTokens)
            .promptTokensDetails(promptTokensDetails)
            .totalTokens(totalTokens)
            .createdAt(LocalDateTime.now())
            .build();
        messageMapper.insert(message);
        return message;
    }

    private List<AssistantMessage> loadContextMessages(Long sessionId) {
        LambdaQueryWrapper<AssistantMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AssistantMessage::getSessionId, sessionId)
            .orderByAsc(AssistantMessage::getCreatedAt)
            .last("LIMIT " + maxContextMessages);
        return messageMapper.selectList(wrapper);
    }

    private List<QwenChatRequest.QwenMessage> buildQwenMessages(List<AssistantMessage> history, String newContent) {
        List<QwenChatRequest.QwenMessage> messages = new ArrayList<>();
        messages.add(QwenChatRequest.QwenMessage.builder()
            .role("system")
            .content("你是一个智能助手，帮助用户解答问题和提供建议。请用简洁、友好的语言回复。")
            .build());

        for (AssistantMessage msg : history) {
            if ("user".equals(msg.getRole())) {
                messages.add(QwenChatRequest.QwenMessage.builder()
                    .role("user")
                    .content(msg.getContent())
                    .build());
            } else if ("assistant".equals(msg.getRole())) {
                QwenChatRequest.QwenMessage.QwenMessageBuilder builder = QwenChatRequest.QwenMessage.builder()
                    .role("assistant")
                    .content(msg.getContent());

                // 如果有思考内容，添加到消息中
                if (msg.getThinkingContent() != null && !msg.getThinkingContent().isEmpty()) {
                    builder.reasoningContent(msg.getThinkingContent());
                }

                messages.add(builder.build());
            }
        }

        messages.add(QwenChatRequest.QwenMessage.builder()
            .role("user")
            .content(newContent)
            .build());

        return messages;
    }

    private void generateTitle(AssistantSession session, String userMessage) {
        try {
            String prompt = String.format("为以下用户的提问提炼主题并生成一个一句话标题（注意：标题中不要含有\"用户\"等主语的描述，只是一段概括）：\n用户：%s", userMessage);

            List<QwenChatRequest.QwenMessage> messages = List.of(
                QwenChatRequest.QwenMessage.builder()
                    .role("system")
                    .content("你是一个标题生成助手，只返回标题文字，不要任何解释。")
                    .build(),
                QwenChatRequest.QwenMessage.builder()
                    .role("user")
                    .content(prompt)
                    .build()
            );

            QwenChatRequest qwenRequest = QwenChatRequest.builder()
                .messages(messages)
                .build();

            // 创建客户端（不需要工具）
            QwenChatClient qwenChatClient = QwenChatClient.builder()
                .config(qwenConfig)
                .restTemplate(qwenRestTemplate)
                .objectMapper(objectMapper)
                .build();

            ChatResponse response = qwenChatClient.chat(qwenRequest);
            String content = response.getContent();
            String title = content.trim()
                .replaceAll("[^\\u4e00-\\u9fa5a-zA-Z0-9\\s]", "");

            session.setTitle(title);
            sessionMapper.updateById(session);
        } catch (Exception e) {
            log.error("生成标题失败", e);
            session.setTitle("新对话");
            sessionMapper.updateById(session);
        }
    }

    private void updateSessionTime(AssistantSession session) {
        session.setUpdatedAt(LocalDateTime.now());
        sessionMapper.updateById(session);
    }

    private AssistantSession validateSessionOwnership(Long sessionId, Long userId) {
        AssistantSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException("会话不存在");
        }
        if (!session.getUserId().equals(userId)) {
            throw new BusinessException("无权访问该会话");
        }
        return session;
    }

    private String getLastMessagePreview(Long sessionId) {
        LambdaQueryWrapper<AssistantMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AssistantMessage::getSessionId, sessionId)
            .orderByDesc(AssistantMessage::getCreatedAt)
            .last("LIMIT 1");

        AssistantMessage message = messageMapper.selectOne(wrapper);
        if (message == null) {
            return null;
        }

        String content = message.getContent();
        return content.length() > 50 ? content.substring(0, 50) + "..." : content;
    }

    /**
     * 发送SSE事件
     */
    private void sendSseEvent(SseEmitter emitter, HttpServletResponse response, SseEvent event) {
        try {
            long sendStartTime = System.currentTimeMillis();
            String json = objectMapper.writeValueAsString(event);
            emitter.send(SseEmitter.event().name("message").data(json));

            // 立即flush响应缓冲区，确保SSE事件实时发送到客户端
            try {
                response.flushBuffer();
                long sendDuration = System.currentTimeMillis() - sendStartTime;
                log.debug("SSE事件发送成功 - 类型: {}, 耗时: {}ms", event.getType(), sendDuration);
            } catch (IOException e) {
                log.warn("Flush响应缓冲区失败", e);
            }

        } catch (Exception e) {
            log.error("SSE发送失败 - 类型: {}", event.getType(), e);
            throw new RuntimeException("SSE发送失败", e);
        }
    }
}
