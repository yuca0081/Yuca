package org.yuca.ai.agent.enhancer;

import lombok.extern.slf4j.Slf4j;
import org.yuca.ai.agent.ChatContext;
import org.yuca.ai.agent.Intent;
import org.yuca.ai.core.message.ChatMessage;
import org.yuca.ai.core.message.UserMessage;
import org.yuca.ai.core.model.ChatRequest;
import org.yuca.ai.core.model.ChatResponse;
import org.yuca.ai.retrieval.RetrievalService;
import org.yuca.ai.retrieval.RetrievedChunk;
import org.yuca.ai.retrieval.TokenBudgetAssembler;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * RAG 增强器
 */
@Slf4j
public class RagEnhancer implements ChatEnhancer {

    private final RetrievalService retrievalService;
    private final TokenBudgetAssembler tokenBudgetAssembler;
    private final Long kbId;
    private final int topN;

    public RagEnhancer(RetrievalService retrievalService,
                       TokenBudgetAssembler tokenBudgetAssembler,
                       Long kbId,
                       int topN) {
        this.retrievalService = retrievalService;
        this.tokenBudgetAssembler = tokenBudgetAssembler;
        this.kbId = kbId;
        this.topN = topN;
    }

    @Override
    public int order() {
        return 1;
    }

    @Override
    public ChatRequest before(ChatRequest request, ChatContext context) {
        // 意图路由：仅 QA / UNKNOWN（或未启用意图识别的 null）才跑 RAG
        // CHITCHAT / TASK / CREATION 跳过，省 embedding + rerank API 成本
        Intent intent = context.getIntent();
        if (intent != null && intent != Intent.QA && intent != Intent.UNKNOWN) {
            log.debug("RagEnhancer 跳过: intent={}", intent);
            return request;
        }

        String query = extractLastUserMessage(request.messages());
        if (query == null || query.isBlank()) {
            return request;
        }

        List<RetrievedChunk> chunks = retrievalService.retrieve(query, kbId, topN);
        if (chunks.isEmpty()) {
            log.debug("RagEnhancer: 未检索到相关内容, query={}", query);
            return request;
        }

        String ragContext = tokenBudgetAssembler.assemble(chunks);
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(UserMessage.from(ragContext));
        messages.addAll(request.messages());

        log.debug("RagEnhancer: 检索到 {} 条相关内容, query={}", chunks.size(), query);
        return ChatRequest.builder()
                .messages(messages)
                .parameters(request.parameters())
                .toolSpecifications(request.toolSpecifications())
                .build();
    }

    @Override
    public void after(ChatResponse response, ChatContext context) {
        // 无操作
    }

    private String extractLastUserMessage(List<ChatMessage> messages) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            if (messages.get(i) instanceof UserMessage userMsg) {
                return userMsg.singleText();
            }
        }
        return null;
    }
}
