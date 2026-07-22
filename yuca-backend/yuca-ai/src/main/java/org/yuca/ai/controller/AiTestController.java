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
import org.yuca.ai.retrieval.RetrievalService;
import org.yuca.ai.retrieval.RetrievedChunk;
import org.yuca.common.response.Result;

import java.util.List;
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

    @Resource
    private RetrievalService retrievalService;

    @GetMapping("/chatModel")
    public String chatModel(String message) {
        AiProperties.ProviderConfig dashscope = aiProperties.getDashscope();
        QwenChatModel model = new QwenChatModel(
                dashscope.getBaseUrl(),
                dashscope.getApiKey(),
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

    /**
     * 测试 RAG 检索效果（不调 LLM，只看检索阶段召回的切片）
     * GET /api/ai/test/retrieve?query=xxx&kbId=1&topN=5
     *
     * <p>用于评估：
     * <ul>
     *   <li>双路召回 + RRF 融合后顺序是否合理</li>
     *   <li>Cross-Encoder rerank 是否生效（score 从 RRF 小数变为 rerank 相关度，通常 0-1）</li>
     *   <li>章节树切片是否生效（需按 chunkId 查 knowledge_chunk 表看 title/breadcrumb）</li>
     * </ul>
     *
     * <p>对比实验：把 application.yml 的 yuca.ai.rerank.enabled 设为 false 重启，
     * 同一 query 再调一次，比较 score 数值范围和顺序变化。
     */
    @GetMapping("/retrieve")
    public Result<List<RetrievedChunk>> retrieve(
            @RequestParam String query,
            @RequestParam Long kbId,
            @RequestParam(defaultValue = "5") int topN) {
        List<RetrievedChunk> chunks = retrievalService.retrieve(query, kbId, topN);
        return Result.success(chunks);
    }
}
