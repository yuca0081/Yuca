package org.yuca.knowledge.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.yuca.ai.embedding.EmbeddingService;
import org.yuca.knowledge.entity.KnowledgeVocabulary;
import org.yuca.knowledge.mapper.KnowledgeVocabularyMapper;

import java.util.List;

/**
 * 查询扩展服务（#8：基于 Embedding 的同义词自动发现）。
 *
 * <p>输入用户原始 query，输出拼接了 top-K 近义词的扩展串（"原query OR 同义词1 OR 同义词2"）。
 * 扩展后的串仅喂给 BM25 路（{@code websearch_to_tsquery} 支持 OR）；
 * 向量检索路保持原 query——语义捕获已足够，再做扩展反而引入噪声。
 *
 * <p>失败降级：embedding 调用或 DB 异常时，catch + WARN，返回原 query，
 * 检索流程不因扩展失败阻塞。
 *
 * <p>开关：{@code yuca.knowledge.query-expansion.enabled=false} 时直接返回原 query，
 * 不调 embedding 不查 DB。
 *
 * @author Yuca
 * @since 2026-07-22
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QueryExpansionService {

    private final EmbeddingService embeddingService;
    private final KnowledgeVocabularyMapper vocabularyMapper;

    /** 从词汇表中召回的近义词数量。过大→噪声稀释 BM25 相关性；过小→扩展不足 */
    private static final int EXPANSION_TOP_K = 3;
    /** 向量相似度阈值（1 - 余弦距离）。0.7 是中文概念词的经验值 */
    private static final double SIMILARITY_THRESHOLD = 0.7;

    @Value("${yuca.knowledge.query-expansion.enabled:true}")
    private boolean enabled;

    /**
     * 扩展 query：基于 embedding 从词汇表找 top-K 近义词，拼成 OR 串。
     *
     * @param query 用户原始 query
     * @param kbId  知识库ID
     * @return 形如 {@code "原query OR 同义词1 OR 同义词2"} 的扩展串；未启用或异常时返回原 query
     */
    public String expand(String query, Long kbId) {
        if (!enabled) {
            return query;
        }
        if (query == null || query.isBlank() || kbId == null) {
            return query;
        }

        try {
            Double[] embedding = embeddingService.embedAsDoubleArray(query);
            String embeddingStr = arrayToString(embedding);

            List<KnowledgeVocabulary> synonyms = vocabularyMapper.searchSimilar(
                    kbId, embeddingStr, EXPANSION_TOP_K, SIMILARITY_THRESHOLD);

            if (synonyms.isEmpty()) {
                log.debug("[queryExpansion] 无近义词命中，返回原 query: kbId={}", kbId);
                return query;
            }

            // 用 OR 连接，适配 websearch_to_tsquery 语法
            StringBuilder sb = new StringBuilder(query);
            int added = 0;
            for (KnowledgeVocabulary v : synonyms) {
                String term = v.getTerm();
                // 跳过空、跳过与原 query 等值（大小写不敏感）的词，避免冗余 OR
                if (term == null || term.isBlank()) {
                    continue;
                }
                if (term.equalsIgnoreCase(query)) {
                    continue;
                }
                sb.append(" OR ").append(term);
                added++;
            }

            if (added == 0) {
                return query;
            }
            log.debug("[queryExpansion] 扩展成功: kbId={}, original='{}', expanded='{}'",
                    kbId, query, sb);
            return sb.toString();
        } catch (Exception e) {
            log.warn("[queryExpansion] 扩展失败，降级用原 query: kbId={}, error={}",
                    kbId, e.getMessage());
            return query;
        }
    }

    /** Double 数组转 PG vector 字面量字符串："[1.0,2.0,3.0]" */
    private String arrayToString(Double[] array) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < array.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(array[i]);
        }
        sb.append("]");
        return sb.toString();
    }
}
