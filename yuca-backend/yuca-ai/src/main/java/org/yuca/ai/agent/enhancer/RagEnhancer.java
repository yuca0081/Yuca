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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * RAG 增强器
 */
@Slf4j
public class RagEnhancer implements ChatEnhancer {

    private final RetrievalService retrievalService;
    private final Long kbId;
    private final int topN;

    public RagEnhancer(RetrievalService retrievalService, Long kbId, int topN) {
        this.retrievalService = retrievalService;
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

        String ragContext = buildRagContext(chunks);
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

    private String buildRagContext(List<RetrievedChunk> chunks) {
        StringBuilder sb = new StringBuilder();
        sb.append("以下是从知识库中检索到的参考资料，请基于这些内容回答用户的问题。");
        sb.append("如果参考资料中没有相关信息，请根据你的知识回答，并说明这不是来自知识库的内容。\n\n");

        for (int i = 0; i < chunks.size(); i++) {
            RetrievedChunk chunk = chunks.get(i);
            sb.append("【参考资料 ").append(i + 1).append("】\n");
            sb.append(chunk.getContent()).append("\n\n");
        }

        return sb.toString();
    }
}
