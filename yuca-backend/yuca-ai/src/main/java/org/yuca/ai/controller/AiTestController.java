package org.yuca.ai.controller;

import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.yuca.ai.agent.Agent;
import org.yuca.ai.agent.AgentFactory;
import org.yuca.ai.agent.ChatContext;
import org.yuca.ai.config.AiProperties;
import org.yuca.ai.core.message.UserMessage;
import org.yuca.ai.core.model.ChatRequest;
import org.yuca.ai.core.model.ChatResponse;
import org.yuca.ai.core.provider.qwen.QwenChatModel;
import org.yuca.common.response.Result;

import java.util.Map;

/**
 * AI 功能测试接口（Postman 调试用）
 */
@RestController
@RequestMapping("/ai/test")
@RequiredArgsConstructor
public class AiTestController {

    @Resource
    private AgentFactory agentFactory;

    @Resource
    private AiProperties aiProperties;

    @GetMapping("/chatModel")
    public String chatModel(String message) {
        AiProperties.ProviderConfig dashscope = aiProperties.getDashscope();
        QwenChatModel model = new QwenChatModel(
                dashscope.getBaseUrl(),
                "sk-4632c16e41c64e738d3b4147aa58581f",
                "qwen3.5-flash");
        ChatRequest request = ChatRequest.builder()
                .messages(java.util.List.of(UserMessage.from(message)))
                .build();
        ChatResponse chat = model.chat(request);
        return chat.aiMessage().text();
    }

    /**
     * 简单对话（无历史）
     * POST /api/ai/test/chat
     */
    @PostMapping("/chat")
    public Result<String> chat(@RequestBody Map<String, String> body) {
        String message = body.get("message");
        Agent simpleAgent = agentFactory.simpleAgent();
        UserMessage userMessage = UserMessage.from(message);
        ChatRequest request = ChatRequest.builder().messages(java.util.List.of(userMessage)).build();
        ChatResponse chatResponse = simpleAgent.execute(request);
        String text = chatResponse.aiMessage().text();
        System.out.println(text);
        return Result.success(text);
    }

    /**
     * 带历史的对话
     * POST /api/ai/test/chat/{sessionId}
     */
    @PostMapping("/chat/{sessionId}")
    public Result<String> chatWithHistory(
            @PathVariable String sessionId,
            @RequestBody Map<String, String> body) {
        ChatContext chatContext = new ChatContext();
        chatContext.setSessionId(sessionId);
        Agent agent = agentFactory.defaultAgent(chatContext);
        String message = body.get("message");
        UserMessage userMessage = UserMessage.from(message);
        ChatRequest request = ChatRequest.builder().messages(java.util.List.of(userMessage)).build();
        ChatResponse chatResponse = agent.execute(request);
        return Result.success(chatResponse.aiMessage().text());
    }
}
