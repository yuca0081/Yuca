package org.yuca.ai.agent.enhancer;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.extern.slf4j.Slf4j;
import org.yuca.ai.agent.ChatContext;
import org.yuca.ai.retrieval.RetrievalService;
import org.yuca.ai.retrieval.RetrievedChunk;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * RAG 增强器
 * 在 SystemPrompt 之前执行（order=-10），从知识库检索相关内容注入上下文
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
        return -10;
    }

    @Override
    public ChatRequest before(ChatRequest request, ChatContext context) {
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
        messages.add(SystemMessage.from(ragContext));
        messages.addAll(request.messages());

        log.debug("RagEnhancer: 检索到 {} 条相关内容, query={}", chunks.size(), query);
        return ChatRequest.builder().messages(messages).build();
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
