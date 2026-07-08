package org.yuca.knowledge.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.yuca.ai.embedding.EmbeddingService;
import org.yuca.ai.retrieval.RerankService;
import org.yuca.ai.retrieval.RetrievedChunk;
import org.yuca.ai.retrieval.RetrievalService;
import org.yuca.knowledge.entity.KnowledgeChunk;
import org.yuca.knowledge.mapper.KnowledgeChunkMapper;

import java.util.*;

/**
 * 知识检索服务实现。
 *
 * <p>三级流水线：
 * <ol>
 *   <li>双路召回（向量 + 关键词，Bi-Encoder——快但粗）</li>
 *   <li>RRF 融合（加权倒数排名）</li>
 *   <li>Cross-Encoder 精排（可选——慢但准，对 MRR 提升最大）</li>
 * </ol>
 *
 * <p>rerankService 通过 {@link ObjectProvider} 注入，yuca.ai.rerank.enabled=false 时为 null，
 * 自动降级为纯 RRF 路径。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeRetrievalService implements RetrievalService {

    private final KnowledgeChunkMapper chunkMapper;
    private final EmbeddingService embeddingService;
    private final ObjectProvider<RerankService> rerankServiceProvider;

    /** 召回阶段每路 topK（reranker 需要更大候选池，从 10 提升到 20） */
    private static final int SEARCH_TOP_K = 20;
    /** 送入 reranker 的候选池大小。文章实验：太大稀释 reranker 注意力，R@5 反降 */
    private static final int RERANK_CANDIDATE_POOL = 20;
    private static final double VECTOR_THRESHOLD = 0.3;
    private static final int RRF_K = 60;

    @Override
    public List<RetrievedChunk> retrieve(String query, Long kbId, int topN) {
        // 1. 生成查询向量
        Double[] queryEmbedding = embeddingService.embedAsDoubleArray(query);
        String embeddingStr = arrayToString(queryEmbedding);

        // 2. 并行双路召回（Bi-Encoder）
        List<KnowledgeChunk> vectorResults = chunkMapper.searchSimilar(
                kbId, embeddingStr, SEARCH_TOP_K, VECTOR_THRESHOLD);
        List<KnowledgeChunk> keywordResults = chunkMapper.searchByKeyword(
                kbId, query, SEARCH_TOP_K);

        log.debug("双路召回完成: 向量={}, 关键词={}", vectorResults.size(), keywordResults.size());

        // 3. RRF 融合（取前 poolSize 条作为 reranker 候选池）
        int poolSize = Math.max(topN, RERANK_CANDIDATE_POOL);
        List<RetrievedChunk> fused = reciprocalRankFusion(vectorResults, keywordResults, poolSize);

        // 4. Cross-Encoder 精排（可选；失败时 DefaultRerankService 内部降级返回原顺序）
        RerankService rerankService = rerankServiceProvider.getIfAvailable();
        if (rerankService != null && fused.size() > 1) {
            log.debug("启用 Rerank 精排: candidatePool={}", fused.size());
            fused = rerankService.rerank(query, fused);
        } else {
            log.debug("Rerank 未启用或候选不足，走纯 RRF 顺序");
        }

        // 5. 截取 topN 返回
        return fused.stream().limit(topN).toList();
    }

    /**
     * Reciprocal Rank Fusion
     * score(d) = Σ 1/(k + rank_i)
     *
     * @param limit 融合后取前 limit 条（reranker 候选池大小）
     */
    private List<RetrievedChunk> reciprocalRankFusion(
            List<KnowledgeChunk> vectorResults,
            List<KnowledgeChunk> keywordResults,
            int limit) {

        Map<Long, Double> scores = new HashMap<>();
        Map<Long, KnowledgeChunk> chunkMap = new HashMap<>();

        for (int i = 0; i < vectorResults.size(); i++) {
            KnowledgeChunk chunk = vectorResults.get(i);
            scores.merge(chunk.getId(), 1.0 / (RRF_K + i + 1), Double::sum);
            chunkMap.putIfAbsent(chunk.getId(), chunk);
        }

        for (int i = 0; i < keywordResults.size(); i++) {
            KnowledgeChunk chunk = keywordResults.get(i);
            scores.merge(chunk.getId(), 1.0 / (RRF_K + i + 1), Double::sum);
            chunkMap.putIfAbsent(chunk.getId(), chunk);
        }

        return scores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(limit)
                .map(entry -> {
                    KnowledgeChunk chunk = chunkMap.get(entry.getKey());
                    return new RetrievedChunk(
                            chunk.getId(),
                            chunk.getDocId(),
                            chunk.getKbId(),
                            chunk.getContent(),
                            entry.getValue()
                    );
                })
                .toList();
    }

    private String arrayToString(Double[] array) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < array.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(array[i]);
        }
        sb.append("]");
        return sb.toString();
    }
}
