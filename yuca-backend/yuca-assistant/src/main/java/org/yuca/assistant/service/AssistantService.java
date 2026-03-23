package org.yuca.assistant.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.output.Response;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.yuca.ai.model.MessageRole;
import org.yuca.ai.service.LangChain4jService;
import org.yuca.assistant.dto.request.AssistantChatRequest;
import org.yuca.assistant.dto.response.MessageDTO;
import org.yuca.assistant.dto.response.SessionDTO;
import org.yuca.assistant.entity.AssistantMessage;
import org.yuca.assistant.mapper.AssistantSessionMapper;
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
 * 基于 LangChain4j 框架重构
 *
 * @author Yuca
 * @since 2025-01-27
 */
@Service
@Slf4j
public class AssistantService {

    @Autowired
    private LangChain4jService langChain4jService;

    @Autowired
    private ApplicationContext applicationContext;

    private final ObjectMapper objectMapper;
    private final AssistantMessageMapper messageMapper;
    private final AssistantSessionMapper sessionMapper;

    @Value("${assistant.max-context-messages:20}")
    private Integer maxContextMessages;

    /**
     * 处理聊天请求（SSE流式）
     * 基于 LangChain4j 实现
     */
    @Transactional
    public void processChat(Long userId, AssistantChatRequest request, SseEmitter emitter, HttpServletResponse response) {
        long startTime = System.currentTimeMillis();
        log.info("开始处理聊天请求 - 用户ID: {}, 会话ID: {}, 模型: {}",
                userId, request.getSessionId(), request.getModelName());

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

            // 4. 确定使用的模型（从请求中获取，或使用默认值）
            String provider = request.getModelName();
            if (provider == null || provider.isEmpty()) {
                provider = "qwen"; // 默认使用千问
            }

            // 5. 准备工具列表（可选）
            List<Object> tools = new ArrayList<>();
            // 添加时间工具
            tools.add(applicationContext.getBean("timeTools"));
            // 添加计算器工具
            tools.add(applicationContext.getBean("calculatorTools"));

            log.info("使用模型提供商: {}, 可用工具数量: {}", provider, tools.size());

            // 6. 使用 LangChain4j 进行流式聊天
            StringBuilder fullResponse = new StringBuilder();
            long tokenStartTime = System.currentTimeMillis();
            AtomicInteger tokenCount = new AtomicInteger(0);

            // 调用 LangChain4j 流式聊天
            Response<AiMessage> aiResponse = langChain4jService.chatStreamWithResponse(
                    provider,
                    request.getSessionId().toString(),
                    request.getContent(),
                    token -> {
                        int currentTokenNum = tokenCount.incrementAndGet();
                        long tokenTime = System.currentTimeMillis() - tokenStartTime;
                        log.info("发送token #{}: content=\"{}\" (AI返回后{}ms)",
                                currentTokenNum, token, tokenTime);

                        // 发送token到前端
                        fullResponse.append(token);
                        sendSseEvent(emitter, response, new SseEvent.SseTokenEvent(token));
                    }
            );

            // 准备token使用数据
            Integer inputTokens = null;
            Integer outputTokens = null;
            Integer totalTokens = null;

            if (aiResponse != null && aiResponse.tokenUsage() != null) {
                inputTokens = aiResponse.tokenUsage().inputTokenCount();
                outputTokens = aiResponse.tokenUsage().outputTokenCount();
                totalTokens = aiResponse.tokenUsage().totalTokenCount();

                log.info("Token使用统计 - input: {}, output: {}, total: {}",
                        inputTokens, outputTokens, totalTokens);
            }

            // 流式响应完成后，保存AI消息
            AssistantMessage assistantMessage = saveAssistantMessage(
                    request.getSessionId(),
                    fullResponse.toString(),
                    provider,  // 使用模型提供商名称
                    null,  // LangChain4j 基础版本不支持思考内容分离
                    inputTokens,
                    outputTokens,
                    null,  // 不包含详细的 prompt tokens details
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
                generateTitle(session, request.getContent(), provider);
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
            .title(null)
            .build();

        sessionMapper.insert(session);

        return SessionDTO.builder()
            .id(session.getId())
            .title(session.getTitle())
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
                .modelName(msg.getModelName())
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

    private AssistantMessage saveAssistantMessage(Long sessionId, String content, String modelName,
                                                  String thinkingContent,
                                                  Integer inputTokens, Integer outputTokens,
                                                  String promptTokensDetails, Integer totalTokens) {
        AssistantMessage message = AssistantMessage.builder()
            .sessionId(sessionId)
            .role(MessageRole.ASSISTANT.getRole())
            .content(content)
            .modelName(modelName)
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

    private void generateTitle(AssistantSession session, String userMessage, String provider) {
        try {
            String prompt = String.format("为以下用户的提问提炼主题并生成一个一句话标题（注意：标题中不要含有\"用户\"等主语的描述，只是一段概括）：\n用户：%s", userMessage);

            // 使用 LangChain4j 生成标题（同步调用，不需要流式）
            String title = langChain4jService.chat(provider, "title-gen-" + session.getId(), prompt);
            String cleanedTitle = title.trim()
                    .replaceAll("[^\\u4e00-\\u9fa5a-zA-Z0-9\\s]", "");

            session.setTitle(cleanedTitle);
            sessionMapper.updateById(session);

            log.info("生成会话标题成功: {}", cleanedTitle);
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
