package org.yuca.ai.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.yuca.ai.client.AiClient;
import org.yuca.common.response.Result;

import java.util.Map;

/**
 * AI 功能测试接口（Postman 调试用）
 */
@RestController
@RequestMapping("/api/ai/test")
@RequiredArgsConstructor
public class AiTestController {

    private final AiClient aiClient;

    /**
     * 简单对话（无历史）
     * POST /api/ai/test/chat
     */
    @PostMapping("/chat")
    public Result<String> chat(@RequestBody Map<String, String> body) {
        String message = body.get("message");
        String reply = aiClient.chat(message);
        return Result.success(reply);
    }

    /**
     * 带历史的对话
     * POST /api/ai/test/chat/{sessionId}
     */
    @PostMapping("/chat/{sessionId}")
    public Result<String> chatWithHistory(
            @PathVariable String sessionId,
            @RequestBody Map<String, String> body) {
        String message = body.get("message");
        String reply = aiClient.chat(message, sessionId);
        return Result.success(reply);
    }
}
