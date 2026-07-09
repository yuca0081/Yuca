package org.yuca.ai.core.provider.qwen;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * DashScope 重排模型（Cross-Encoder 精排）。
 * 走 DashScope 原生端点 POST /api/v1/services/rerank/text-rerank/text-rerank
 * （rerank 不在 OpenAI 兼容协议里，是 DashScope 专属 API）。
 *
 * <p>召回阶段（向量 + BM25）是 Bi-Encoder——查询与文档独立编码，快但粗；
 * 本模型是 Cross-Encoder——查询与文档拼接精排，慢但准，对 MRR 提升最大。
 *
 * <p>当前默认模型 gte-rerank-v2，单次最多 1024 documents、单 document 不超过 2048 tokens。
 */
@Slf4j
public class QwenRerankModel {

    private static final String RERANK_PATH = "/api/v1/services/rerank/text-rerank/text-rerank";

    private final RestClient restClient;
    private final String modelName;

    public QwenRerankModel(String baseUrl, String apiKey, String modelName) {
        this.modelName = modelName;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    /**
     * 用 Cross-Encoder 对 documents 相对 query 重排序。
     *
     * @param query     用户查询
     * @param documents 候选文档（建议 ≤20，太多会稀释 reranker 注意力）
     * @param topN      返回前 N 条（按 relevance_score 降序）
     * @return 重排结果，size = min(topN, documents.size())
     */
    public List<RerankResult> rerank(String query, List<String> documents, int topN) {
        if (documents.isEmpty()) {
            return List.of();
        }
        Map<String, Object> requestBody = Map.of(
                "model", modelName,
                "input", Map.of(
                        "query", query,
                        "documents", documents),
                "parameters", Map.of(
                        "top_n", Math.min(topN, documents.size()),
                        "return_documents", false)
        );

        if (log.isDebugEnabled()) {
            log.debug("QwenRerankModel 请求: query='{}', docCount={}, topN={}",
                    query, documents.size(), topN);
        }

        JsonNode resp = restClient.post()
                .uri(RERANK_PATH)
                .body(requestBody)
                .retrieve()
                .body(JsonNode.class);

        if (resp == null || resp.path("output").path("results").isMissingNode()) {
            log.warn("QwenRerankModel 响应缺少 output.results: {}", resp);
            return List.of();
        }

        List<RerankResult> results = new ArrayList<>();
        for (JsonNode r : resp.get("output").get("results")) {
            int idx = r.path("index").asInt();
            double score = r.path("relevance_score").asDouble();
            results.add(new RerankResult(idx, score));
        }
        if (log.isDebugEnabled()) {
            log.debug("QwenRerankModel 返回 {} 条结果", results.size());
        }
        return results;
    }

    /**
     * rerank 单条结果：documents 列表里的索引 + 相关度分数。
     */
    public record RerankResult(int index, double relevanceScore) {}
}
