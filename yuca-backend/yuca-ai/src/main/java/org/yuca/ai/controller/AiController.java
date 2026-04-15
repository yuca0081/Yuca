package org.yuca.ai.controller;

import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.yuca.ai.service.ChatService;
import org.yuca.common.annotation.SkipAuth;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
@Slf4j
@SkipAuth
public class AiController {

    private final ChatService chatService;

    @GetMapping("/chat")
    public String chat(@RequestParam String message) {
        return chatService.chat(message);
    }

    @GetMapping("/streamChat")
    public Flux<String> streamChat(@RequestParam String message) {
        log.info("收到流式聊天请求, message={}", message);
        return chatService.streamChat(message);
    }

    @GetMapping("/chatWithMemory")
    public Map<String, Object> chatWithMemory(
            @RequestParam String message,
            @RequestParam(defaultValue = "default") String sessionId) {

        log.info("收到带 memory 的聊天请求, sessionId={}, message={}", sessionId, message);

        try {
            ChatResponse response = chatService.chatWithMemory(message, sessionId);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("sessionId", sessionId);
            result.put("userMessage", message);
            result.put("response", response);

            return result;
        } catch (Exception e) {
            log.error("聊天处理失败", e);

            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            error.put("sessionId", sessionId);
            return error;
        }
    }
}
