package org.yuca.ai.retrieval;

import java.util.List;

/**
 * Cross-Encoder 重排服务。
 *
 * <p>召回阶段（向量 + BM25）用 Bi-Encoder——查询与文档独立编码，快但粗；
 * 重排阶段用 Cross-Encoder——查询与文档拼接精排，慢但准。Rerank 对 Recall 贡献小，
 * 但对 MRR（首位命中率）贡献最大，是 RAG 性价比最高的单项优化。
 *
 * <p>实现应做失败降级——rerank 异常时返回原 candidates，绝不阻塞主流程。
 */
public interface RerankService {

    /**
     * 对召回阶段产出的候选 chunk 重排序。
     *
     * @param query      用户查询
     * @param candidates 召回 + RRF 融合后的候选列表（建议 20 条左右，太多会稀释 reranker 注意力）
     * @return 按 Cross-Encoder 相关度降序的列表，size 与入参一致
     */
    List<RetrievedChunk> rerank(String query, List<RetrievedChunk> candidates);
}
