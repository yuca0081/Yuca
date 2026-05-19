package org.yuca.knowledge.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.yuca.ai.embedding.EmbeddingService;
import org.yuca.ai.retrieval.RetrievedChunk;
import org.yuca.ai.retrieval.RetrievalService;
import org.yuca.knowledge.entity.KnowledgeChunk;
import org.yuca.knowledge.mapper.KnowledgeChunkMapper;

import java.util.*;
import java.util.stream.IntStream;

/**
 * 知识检索服务实现
 * 双路检索（向量 + 关键词）+ RRF 融合
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeRetrievalService implements RetrievalService {

    private final KnowledgeChunkMapper chunkMapper;
    private final EmbeddingService embeddingService;

    private static final int SEARCH_TOP_K = 10;
    private static final double VECTOR_THRESHOLD = 0.3;
    private static final int RRF_K = 60;

    @Override
    public List<RetrievedChunk> retrieve(String query, Long kbId, int topN) {
        // 1. 生成查询向量
        Double[] queryEmbedding = embeddingService.embedAsDoubleArray(query);
        String embeddingStr = arrayToString(queryEmbedding);

        // 2. 并行双路检索
        List<KnowledgeChunk> vectorResults = chunkMapper.searchSimilar(
                kbId, embeddingStr, SEARCH_TOP_K, VECTOR_THRESHOLD);
        List<KnowledgeChunk> keywordResults = chunkMapper.searchByKeyword(
                kbId, query, SEARCH_TOP_K);

        log.debug("双路检索完成: 向量={}, 关键词={}", vectorResults.size(), keywordResults.size());

        // 3. RRF 融合
        return reciprocalRankFusion(vectorResults, keywordResults, topN);
    }

    /**
     * Reciprocal Rank Fusion
     * score(d) = Σ 1/(k + rank_i)
     */
    private List<RetrievedChunk> reciprocalRankFusion(
            List<KnowledgeChunk> vectorResults,
            List<KnowledgeChunk> keywordResults,
            int topN) {

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
                .limit(topN)
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
