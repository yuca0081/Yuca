package org.yuca.yuca.ai.client;

import org.yuca.yuca.ai.common.EmbeddingRequest;
import org.yuca.yuca.ai.model.EmbeddingResponse;

import java.util.List;

/**
 * AI 向量嵌入客户端接口
 *
 * <p>提供统一的向量嵌入抽象
 *
 * @author Yuca
 * @since 2025-01-27
 */
public interface AIEmbeddingClient {

    /**
     * 批量生成向量嵌入
     *
     * @param request 嵌入请求
     * @return 嵌入响应
     */
    EmbeddingResponse embed(EmbeddingRequest request);

    /**
     * 为单个文本生成向量嵌入
     *
     * @param text 输入文本
     * @return 向量嵌入数组（Double类型保持精度）
     */
    default Double[] embed(String text) {
        return embed(EmbeddingRequest.builder()
            .inputs(List.of(text))
            .build())
            .getResults()
            .get(0)
            .getEmbedding();
    }

    /**
     * 批量生成向量嵌入
     *
     * @param texts 文本列表
     * @return 向量嵌入列表（Double类型保持精度）
     */
    default List<Double[]> batchEmbed(List<String> texts) {
        return embed(EmbeddingRequest.builder()
            .inputs(texts)
            .build())
            .getResults()
            .stream()
            .map(result -> result.getEmbedding())
            .toList();
    }
}
