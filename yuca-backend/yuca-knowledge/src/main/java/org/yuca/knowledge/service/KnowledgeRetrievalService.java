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
 * <p>四级流水线：
 * <ol>
 *   <li>双路召回（向量 + 关键词，Bi-Encoder——快但粗）</li>
 *   <li>RRF 融合（加权倒数排名）</li>
 *   <li>Cross-Encoder 精排（可选——慢但准，对 MRR 提升最大）</li>
 *   <li>保守父子注入（按 parent_id 扩展高分节点的子节点，分数 × 0.5 折扣）</li>
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
    /** 保守父子注入：每个高分父节点最多追加的子节点数 */
    private static final int MAX_CHILDREN_PER_PARENT = 2;
    /** 保守父子注入：子节点分数 = 父节点分数 × 折扣，保证父优先于子 */
    private static final double CHILD_SCORE_DISCOUNT = 0.5;

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

        // 3. RRF 融合（取前 poolSize 条作为 reranker 候选池），同时收集 parentId 映射供父子注入用
        int poolSize = Math.max(topN, RERANK_CANDIDATE_POOL);
        Map<Long, Long> parentIdMap = new HashMap<>();
        List<RetrievedChunk> fused = reciprocalRankFusion(vectorResults, keywordResults, poolSize, parentIdMap);

        // 4. Cross-Encoder 精排（可选；失败时 DefaultRerankService 内部降级返回原顺序）
        RerankService rerankService = rerankServiceProvider.getIfAvailable();
        if (rerankService != null && fused.size() > 1) {
            log.debug("启用 Rerank 精排: candidatePool={}", fused.size());
            fused = rerankService.rerank(query, fused);
        } else {
            log.debug("Rerank 未启用或候选不足，走纯 RRF 顺序");
        }

        // 5. 保守父子注入：对前 topN 个高分父节点按 parent_id 反查子节点扩展语义
        fused = injectChildren(fused, parentIdMap, topN);

        // 6. 截取 topN 返回
        return fused.stream().limit(topN).toList();
    }

    /**
     * Reciprocal Rank Fusion
     * score(d) = Σ 1/(k + rank_i)
     *
     * @param limit        融合后取前 limit 条（reranker 候选池大小）
     * @param parentIdMap  出参：收集所有 chunk 的 parentId 映射（chunkId → parentId），
     *                     供后续 {@link #injectChildren} 用。扁平切片 / 根节点的 parentId 为 null，不会进入此 map
     */
    private List<RetrievedChunk> reciprocalRankFusion(
            List<KnowledgeChunk> vectorResults,
            List<KnowledgeChunk> keywordResults,
            int limit,
            Map<Long, Long> parentIdMap) {

        Map<Long, Double> scores = new HashMap<>();
        Map<Long, KnowledgeChunk> chunkMap = new HashMap<>();

        for (int i = 0; i < vectorResults.size(); i++) {
            KnowledgeChunk chunk = vectorResults.get(i);
            scores.merge(chunk.getId(), 1.0 / (RRF_K + i + 1), Double::sum);
            chunkMap.putIfAbsent(chunk.getId(), chunk);
            collectParentId(chunk, parentIdMap);
        }

        for (int i = 0; i < keywordResults.size(); i++) {
            KnowledgeChunk chunk = keywordResults.get(i);
            scores.merge(chunk.getId(), 1.0 / (RRF_K + i + 1), Double::sum);
            chunkMap.putIfAbsent(chunk.getId(), chunk);
            collectParentId(chunk, parentIdMap);
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
                            entry.getValue(),
                            chunk.getSummary()
                    );
                })
                .toList();
    }

    /** 收集 chunk 的 parentId 到映射表；扁平切片和根节点的 parentId 为 null，跳过 */
    private void collectParentId(KnowledgeChunk chunk, Map<Long, Long> parentIdMap) {
        if (chunk.getParentId() != null) {
            parentIdMap.put(chunk.getId(), chunk.getParentId());
        }
    }

    /**
     * 保守父子注入：rerank 后、limit 前对前 topN 个高分父节点扩展子节点。
     *
     * <p>简历项目策略："Rerank后对高分节点从章节树直接查表取最多2个子节点加入结果，
     * 子节点分数 = 父节点分数 × 0.5折扣"。子节点必然排在自己父节点之后（分数减半），
     * 但可能挤掉其他低分节点——设计如此，扩展语义的代价。
     *
     * <p>去重：子节点若已在候选池（被独立召回）则跳过，避免同一 chunk 多次出现。
     */
    private List<RetrievedChunk> injectChildren(List<RetrievedChunk> fused,
                                                Map<Long, Long> parentIdMap,
                                                int topN) {
        if (parentIdMap.isEmpty()) {
            return fused;
        }

        Set<Long> existingIds = new HashSet<>();
        for (RetrievedChunk c : fused) {
            existingIds.add(c.getChunkId());
        }

        List<RetrievedChunk> injected = new ArrayList<>();
        int parentCount = Math.min(topN, fused.size());

        for (int i = 0; i < parentCount; i++) {
            RetrievedChunk parent = fused.get(i);
            // parentIdMap 不含该 chunkId → 扁平切片或根节点，跳过
            if (!parentIdMap.containsKey(parent.getChunkId())) {
                continue;
            }

            List<KnowledgeChunk> children = chunkMapper.selectChildrenByParentId(
                    parent.getChunkId(), MAX_CHILDREN_PER_PARENT);
            for (KnowledgeChunk child : children) {
                if (existingIds.contains(child.getId())) {
                    continue;
                }
                injected.add(new RetrievedChunk(
                        child.getId(),
                        child.getDocId(),
                        child.getKbId(),
                        child.getContent(),
                        parent.getScore() * CHILD_SCORE_DISCOUNT,
                        child.getSummary()
                ));
                existingIds.add(child.getId());
            }
        }

        if (injected.isEmpty()) {
            return fused;
        }

        List<RetrievedChunk> result = new ArrayList<>(fused.size() + injected.size());
        result.addAll(fused);
        result.addAll(injected);
        result.sort(Comparator.comparingDouble(RetrievedChunk::getScore).reversed());

        log.debug("父子注入完成: 原候选={}, 注入={}, 合并后={}", fused.size(), injected.size(), result.size());
        return result;
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
