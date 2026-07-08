package org.yuca.ai.retrieval;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.yuca.ai.core.provider.qwen.QwenRerankModel;

import java.util.ArrayList;
import java.util.List;

/**
 * 默认 rerank 实现：调用 {@link QwenRerankModel} 对候选 chunk 做 Cross-Encoder 精排。
 *
 * <p><b>失败降级</b>：rerank 是远程 API 调用，可能因网络、限流、鉴权等原因失败。
 * 本实现将整个 rerank 过程包在 try-catch 里，异常时 log.warn 并返回原 candidates，
 * 让上层走 RRF 融合结果——rerank 是优化项，绝不阻塞主流程。
 */
@Slf4j
@RequiredArgsConstructor
public class DefaultRerankService implements RerankService {

    private final QwenRerankModel rerankModel;

    @Override
    public List<RetrievedChunk> rerank(String query, List<RetrievedChunk> candidates) {
        if (candidates == null || candidates.size() <= 1) {
            return candidates == null ? List.of() : candidates;
        }

        try {
            List<String> docs = candidates.stream()
                    .map(RetrievedChunk::getContent)
                    .toList();

            List<QwenRerankModel.RerankResult> results =
                    rerankModel.rerank(query, docs, candidates.size());

            if (results.isEmpty()) {
                log.warn("Rerank 返回空结果，降级使用原 RRF 顺序");
                return candidates;
            }

            // 按 rerank 返回的顺序（已按 relevance_score 降序）重建 chunk 列表，
            // 并把 rerank 分数写回 score 字段覆盖 RRF 分数。
            List<RetrievedChunk> reranked = new ArrayList<>(results.size());
            for (QwenRerankModel.RerankResult r : results) {
                if (r.index() >= 0 && r.index() < candidates.size()) {
                    RetrievedChunk chunk = candidates.get(r.index());
                    chunk.setScore(r.relevanceScore());
                    reranked.add(chunk);
                }
            }

            // 防御性：如果 reranker 漏返了部分索引，把剩余的原顺序追加到尾部
            if (reranked.size() < candidates.size()) {
                log.debug("Rerank 返回 {} 条，原 candidates {} 条，剩余按原顺序追加",
                        reranked.size(), candidates.size());
                for (int i = 0; i < candidates.size(); i++) {
                    final int idx = i;
                    boolean alreadyIncluded = reranked.stream()
                            .anyMatch(c -> c == candidates.get(idx));
                    if (!alreadyIncluded) {
                        reranked.add(candidates.get(idx));
                    }
                }
            }

            log.debug("Rerank 完成: {} 条候选 → {} 条精排结果", candidates.size(), reranked.size());
            return reranked;
        } catch (Exception e) {
            log.warn("Rerank 调用失败，降级使用原 RRF 顺序。query='{}', error={}",
                    query, e.getMessage());
            return candidates;
        }
    }
}
