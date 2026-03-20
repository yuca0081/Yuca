package org.yuca.assistant.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.yuca.assistant.dto.request.AssistantChatRequest;
import org.yuca.assistant.dto.request.CreateSessionRequest;
import org.yuca.assistant.dto.response.SessionDTO;
import org.yuca.assistant.service.AssistantService;
import org.yuca.common.response.Result;
import org.yuca.infrastructure.security.SecurityUtils;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * AI助手控制器
 *
 * @author Yuca
 * @since 2025-01-27
 */
@RestController
@RequestMapping("/assistant")
@RequiredArgsConstructor
@Slf4j
public class AssistantController {

    private final AssistantService assistantService;
    private final Executor asyncExecutor;

    /**
     * 发送消息（SSE流式响应）
     */
    @PostMapping("/chat")
    public SseEmitter chat(@RequestBody @Valid AssistantChatRequest request, HttpServletResponse response) throws IOException {
        Long userId = SecurityUtils.getCurrentUserId();
        log.info("收到聊天请求 - 用户ID: {}, 会话ID: {}", userId, request.getSessionId());

        // 设置SSE必需的响应头
        response.setContentType("text/event-stream");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");
        response.setHeader("X-Accel-Buffering", "no"); // 禁用Nginx缓冲

        // 禁用响应缓冲，确保SSE事件立即发送
        response.setBufferSize(0);
        response.flushBuffer();

        // 创建SSE发射器（5分钟超时，防止AI响应时间过长导致超时）
        SseEmitter emitter = new SseEmitter(300000L);

        // 添加错误和完成回调
        emitter.onError(ex -> log.error("SSE连接错误", ex));
        emitter.onTimeout(() -> log.warn("SSE连接超时"));
        emitter.onCompletion(() -> log.info("SSE连接完成"));

        // 这样客户端可以立即开始接收SSE事件，而不是等待AI响应完成
        CompletableFuture.runAsync(() -> {
            try {
                assistantService.processChat(userId, request, emitter, response);
            } catch (Exception e) {
                log.error("处理聊天请求失败", e);
                try {
                    emitter.send(SseEmitter.event()
                        .name("message")
                        .data("{\"type\":\"error\",\"message\":\"" + e.getMessage() + "\"}"));
                    response.flushBuffer();
                    emitter.complete();
                } catch (IOException ex) {
                    log.error("发送错误事件失败", ex);
                    emitter.completeWithError(ex);
                }
            }
        }, asyncExecutor);
        return emitter;
    }

    /**
     * 获取会话列表
     */
    @GetMapping("/sessions")
    public Result<List<SessionDTO>> getSessions(
        @RequestParam(defaultValue = "0") int offset,
        @RequestParam(defaultValue = "20") int limit) {
        Long userId = SecurityUtils.getCurrentUserId();
        List<SessionDTO> sessions = assistantService.getSessions(userId, offset, limit);
        return Result.success(sessions);
    }

    /**
     * 创建新会话
     */
    @PostMapping("/session")
    public Result<SessionDTO> createSession(@RequestBody CreateSessionRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        SessionDTO session = assistantService.createSession(userId, request.getModelName());
        return Result.success("会话创建成功", session);
    }

    /**
     * 获取会话详情（含消息）
     */
    @GetMapping("/session/{id}")
    public Result<SessionDTO> getSessionDetail(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        SessionDTO session = assistantService.getSessionDetail(id, userId);
        return Result.success(session);
    }

    /**
     * 删除会话
     */
    @DeleteMapping("/session/{id}")
    public Result<Void> deleteSession(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        assistantService.deleteSession(id, userId);
        return Result.success("会话删除成功", null);
    }
}
