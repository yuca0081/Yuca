package org.yuca.diet.controller;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.yuca.ai.agent.ChatContext;
import org.yuca.common.response.Result;
import org.yuca.diet.agent.DietAgentFactory;
import org.yuca.infrastructure.security.SecurityUtils;

import java.util.List;

/**
 * 饮食助手控制器
 */
@RestController
@RequestMapping("/diet/assistant")
@RequiredArgsConstructor
@Slf4j
public class DietAssistantController {

    private final DietAgentFactory dietAgentFactory;

    @PostMapping("/chat")
    public Result<ChatResponseDTO> chat(@RequestBody @Valid DietChatRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        log.info("饮食助手聊天请求 - 用户ID: {}", userId);

        ChatContext context = new ChatContext();
        context.setSessionId(request.getSessionId());
        context.setUserId(userId);

        ChatRequest chatRequest = ChatRequest.builder()
                .messages(List.of(new UserMessage(request.getContent())))
                .build();

        ChatResponse response = dietAgentFactory.createDietAgent(context)
                .execute(chatRequest);

        String content = response.aiMessage() != null ? response.aiMessage().text() : "";
        return Result.success(new ChatResponseDTO(content));
    }

    @Data
    public static class DietChatRequest {
        private String sessionId;

        @NotBlank(message = "消息内容不能为空")
        private String content;
    }

    @Data
    public static class ChatResponseDTO {
        private final String content;
    }
}
