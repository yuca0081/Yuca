package org.yuca.note.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.yuca.ai.core.message.UserMessage;
import org.yuca.ai.core.model.ChatRequest;
import org.yuca.ai.core.model.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.yuca.ai.agent.Agent;
import org.yuca.ai.agent.ChatContext;
import org.yuca.common.response.Result;
import org.yuca.infrastructure.security.SecurityUtils;
import org.yuca.note.agent.NoteAgentFactory;

import java.util.List;

/**
 * 笔记 AI 助手控制器
 */
@RestController
@RequestMapping("/note/assistant")
@RequiredArgsConstructor
@Slf4j
public class NoteAssistantController {

    private final NoteAgentFactory noteAgentFactory;

    /**
     * 笔记 AI 对话
     */
    @PostMapping("/chat")
    public Result<ChatResponseDTO> chat(@RequestBody @Valid NoteChatRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        log.info("笔记助手聊天请求 - 用户ID: {}", userId);

        ChatContext context = new ChatContext();
        context.setSessionId(request.getSessionId());
        context.setUserId(userId);

        ChatRequest chatRequest = ChatRequest.builder()
                .messages(List.of(new UserMessage(request.getContent())))
                .build();

        Agent agent = noteAgentFactory.createNoteAgent(context);
        ChatResponse response = agent.execute(chatRequest);

        String content = response.aiMessage() != null ? response.aiMessage().text() : "";
        return Result.success(new ChatResponseDTO(content));
    }

    /**
     * 文档一键操作（总结/翻译/润色/扩写/生成大纲）
     */
    @PostMapping("/doc-action")
    public Result<DocActionResponseDTO> docAction(@RequestBody @Valid DocActionRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        log.info("文档操作请求 - 用户ID: {}, 操作: {}, 笔记ID: {}", userId, request.getAction(), request.getNoteItemId());

        Agent agent = noteAgentFactory.createDocActionAgent(
                request.getAction(),
                request.getTitle() != null ? request.getTitle() : "",
                request.getContent() != null ? request.getContent() : "",
                request.getTargetLanguage()
        );

        ChatRequest chatRequest = noteAgentFactory.buildDocActionRequest(
                request.getTitle() != null ? request.getTitle() : "",
                request.getContent() != null ? request.getContent() : ""
        );

        ChatResponse response = agent.execute(chatRequest);

        String result = response.aiMessage() != null ? response.aiMessage().text() : "";
        return Result.success(new DocActionResponseDTO(result, request.getAction()));
    }

    // ========== DTOs ==========

    @Data
    public static class NoteChatRequest {
        private String sessionId;

        @NotBlank(message = "消息内容不能为空")
        private String content;
    }

    @Data
    public static class ChatResponseDTO {
        private final String content;
    }

    @Data
    public static class DocActionRequest {
        @NotNull(message = "笔记ID不能为空")
        private Long noteItemId;

        @NotBlank(message = "操作类型不能为空")
        private String action; // SUMMARIZE, TRANSLATE, POLISH, EXPAND, OUTLINE

        private String targetLanguage; // 翻译目标语言，action=TRANSLATE 时必填

        private String title; // 文档标题（前端传入）

        private String content; // 文档内容（前端传入）
    }

    @Data
    public static class DocActionResponseDTO {
        private final String result;
        private final String action;
    }
}
