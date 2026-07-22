package org.yuca.knowledge.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.yuca.ai.embedding.EmbeddingService;
import org.yuca.ai.retrieval.MetadataFilter;
import org.yuca.ai.retrieval.RerankService;
import org.yuca.ai.retrieval.RetrievedChunk;
import org.yuca.ai.retrieval.RetrievalService;
import org.yuca.knowledge.entity.KnowledgeChunk;
import org.yuca.knowledge.mapper.KnowledgeChunkMapper;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 知识检索服务实现。
 *
 * <p>四级流水线：
 * <ol>
 *   <li>多查询 × 双路召回（每个子 query 做向量 + BM25）。BM25 路会先调
 *       {@link QueryExpansionService}（#8）基于 embedding 找 top-K 同义词拼成 OR 串；
 *       子 query 由 {@link MultiQueryExpander}（#9）调 LLM 改写自不同视角生成。
 *       两开关独立，关闭 #9 时退化为单 query，关闭 #8 时 BM25 路用原 query。
 *       #10 元数据过滤：{@link MetadataFilter} 透传到每路召回的 SQL，filter=null 时退化为无 JOIN 查询</li>
 *   <li>RRF 融合（跨子 query 累加倒数排名，保证每个子 query 的 rank 都从 1 起算）</li>
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
    private final QueryExpansionService queryExpansionService;
    private final MultiQueryExpander multiQueryExpander;

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
    /** #10 MetadataFilter.attrs 的 key 白名单：防 SQL 注入（${} 拼接必须校验） */
    private static final Pattern ATTRS_KEY_PATTERN = Pattern.compile("^[A-Za-z0-9_]{1,50}$");

    @Override
    public List<RetrievedChunk> retrieve(String query, Long kbId, int topN) {
        return retrieve(query, kbId, topN, null);
    }

    /**
     * 带元数据过滤的检索（#10）。覆盖 {@link RetrievalService} 的 default 实现，
     * 把 filter 透传到每个子 query 的双路召回 SQL。
     */
    @Override
    public List<RetrievedChunk> retrieve(String query, Long kbId, int topN, MetadataFilter filter) {
        // 0. 预处理 filter：校验 attrs key 防注入；空 filter 归一化为 null（走无 JOIN 路径）
        MetadataFilter normalizedFilter = normalizeFilter(filter);

        // 1. 组装子 query 列表：原 query 永远第一位（兜底精确匹配），追加 N 个 LLM 改写（#9）
        List<String> queries = new ArrayList<>();
        queries.add(query);
        List<String> rewrites = multiQueryExpander.rewrite(query);
        for (String r : rewrites) {
            if (!queries.contains(r)) {
                queries.add(r);
            }
        }
        log.debug("[retrieve] 多查询准备完成: count={}, queries={}, hasFilter={}",
                queries.size(), queries, normalizedFilter != null);

        // 2. 循环每个子 query 做双路召回。filter 透传给每路 SQL
        List<List<KnowledgeChunk>> vectorResultsPerQuery = new ArrayList<>(queries.size());
        List<List<KnowledgeChunk>> keywordResultsPerQuery = new ArrayList<>(queries.size());
        for (String subQuery : queries) {
            Double[] subEmb = embeddingService.embedAsDoubleArray(subQuery);
            String subEmbStr = arrayToString(subEmb);
            List<KnowledgeChunk> v = chunkMapper.searchSimilarWithFilter(
                    kbId, subEmbStr, SEARCH_TOP_K, VECTOR_THRESHOLD, normalizedFilter);
            String expandedSub = queryExpansionService.expand(subQuery, kbId);
            List<KnowledgeChunk> k = chunkMapper.searchByKeywordWithFilter(
                    kbId, expandedSub, SEARCH_TOP_K, normalizedFilter);
            vectorResultsPerQuery.add(v);
            keywordResultsPerQuery.add(k);
        }
        log.debug("[retrieve] 多查询双路召回完成: queryCount={}", queries.size());

        // 3. RRF 融合（每子 query 独立计 rank，跨 query 同 id 累加）
        int poolSize = Math.max(topN, RERANK_CANDIDATE_POOL);
        Map<Long, Long> parentIdMap = new HashMap<>();
        List<RetrievedChunk> fused = reciprocalRankFusionMultiQuery(
                vectorResultsPerQuery, keywordResultsPerQuery, poolSize, parentIdMap);

        // 4. Cross-Encoder 精排（用原始 query，不是子 query）
        RerankService rerankService = rerankServiceProvider.getIfAvailable();
        if (rerankService != null && fused.size() > 1) {
            log.debug("启用 Rerank 精排: candidatePool={}", fused.size());
            fused = rerankService.rerank(query, fused);
        } else {
            log.debug("Rerank 未启用或候选不足，走纯 RRF 顺序");
        }

        // 5. 保守父子注入
        fused = injectChildren(fused, parentIdMap, topN);

        // 6. 截取 topN 返回
        return fused.stream().limit(topN).toList();
    }

    /**
     * 归一化 filter：全字段空时返回 null（让 mapper 的 {@code <if>} 走无 JOIN 路径），
     * attrs key 用白名单校验防 SQL 注入（mapper 里用 {@code ${}} 拼接 key）。
     */
    private MetadataFilter normalizeFilter(MetadataFilter filter) {
        if (filter == null) {
            return null;
        }
        boolean hasTags = filter.getTags() != null && !filter.getTags().isEmpty();
        boolean hasSource = filter.getSource() != null && !filter.getSource().isBlank();
        boolean hasDateFrom = filter.getDateFrom() != null;
        boolean hasDateTo = filter.getDateTo() != null;
        boolean hasAttrs = filter.getAttrs() != null && !filter.getAttrs().isEmpty();
        if (!hasTags && !hasSource && !hasDateFrom && !hasDateTo && !hasAttrs) {
            return null;
        }
        // attrs key 白名单校验
        if (hasAttrs) {
            for (String key : filter.getAttrs().keySet()) {
                if (key == null || !ATTRS_KEY_PATTERN.matcher(key).matches()) {
                    throw new IllegalArgumentException(
                            "Invalid metadata attr key: '" + key + "', must match [A-Za-z0-9_]{1,50}");
                }
            }
        }
        return filter;
    }

    /**
     * Reciprocal Rank Fusion（多查询版本）。
     *
     * <p>公式：{@code score(d) = Σ_{q∈queries} Σ_{path∈{vector,keyword}} 1/(k + rank_{q,path}(d))}
     *
     * <p>关键差异：每子 query 的每路召回都独立从 rank=1 起算，跨 query 同 chunk 的分数累加。
     * 这避免了"拼接大 list 导致子 query N 的 chunk rank 偏低"的不公平。
     *
     * @param vectorResultsPerQuery   每子 query 的向量召回结果
     * @param keywordResultsPerQuery  每子 query 的 BM25 召回结果（已过 #8 扩展）
     * @param limit                   融合后取前 limit 条（reranker 候选池大小）
     * @param parentIdMap             出参：收集所有 chunk 的 parentId 映射，供父子注入用
     */
    private List<RetrievedChunk> reciprocalRankFusionMultiQuery(
            List<List<KnowledgeChunk>> vectorResultsPerQuery,
            List<List<KnowledgeChunk>> keywordResultsPerQuery,
            int limit,
            Map<Long, Long> parentIdMap) {

        Map<Long, Double> scores = new HashMap<>();
        Map<Long, KnowledgeChunk> chunkMap = new HashMap<>();

        for (List<KnowledgeChunk> oneQueryVector : vectorResultsPerQuery) {
            for (int i = 0; i < oneQueryVector.size(); i++) {
                KnowledgeChunk chunk = oneQueryVector.get(i);
                scores.merge(chunk.getId(), 1.0 / (RRF_K + i + 1), Double::sum);
                chunkMap.putIfAbsent(chunk.getId(), chunk);
                collectParentId(chunk, parentIdMap);
            }
        }

        for (List<KnowledgeChunk> oneQueryKeyword : keywordResultsPerQuery) {
            for (int i = 0; i < oneQueryKeyword.size(); i++) {
                KnowledgeChunk chunk = oneQueryKeyword.get(i);
                scores.merge(chunk.getId(), 1.0 / (RRF_K + i + 1), Double::sum);
                chunkMap.putIfAbsent(chunk.getId(), chunk);
                collectParentId(chunk, parentIdMap);
            }
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
