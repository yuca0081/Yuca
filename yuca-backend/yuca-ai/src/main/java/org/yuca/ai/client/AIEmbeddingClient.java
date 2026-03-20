package org.yuca.ai.client;

import org.yuca.ai.model.EmbeddingRequest;
import org.yuca.ai.model.EmbeddingResponse;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * AI 向量嵌入客户端接口
 *
 * <p>提供统一的向量嵌入抽象
 * <p>
 * 提供基础方法和便利方法（默认方法），简化调用
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

    // ========== 便利方法 ==========

    /**
     * 为单个文本生成向量嵌入
     *
     * @param text 输入文本
     * @return 向量嵌入数组
     */
    default Double[] embed(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("文本内容不能为空");
        }

        EmbeddingRequest request = EmbeddingRequest.builder()
            .inputs(Collections.singletonList(text))
            .build();

        EmbeddingResponse response = embed(request);
        return response.getEmbeddingArray();
    }

    /**
     * 批量生成向量嵌入
     *
     * @param texts 文本列表
     * @return 向量嵌入列表
     */
    default List<Double[]> batchEmbed(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            throw new IllegalArgumentException("文本列表不能为空");
        }

        EmbeddingRequest request = EmbeddingRequest.builder()
            .inputs(texts)
            .build();

        EmbeddingResponse response = embed(request);

        // 转换为 Double[] 列表
        return response.getData().stream()
            .map(data -> data.getEmbedding().toArray(new Double[0]))
            .toList();
    }

    // ========== 静态工具方法 ==========

    /**
     * 计算余弦相似度
     *
     * @param vec1 向量1
     * @param vec2 向量2
     * @return 相似度得分（0-1之间，1表示完全相同）
     */
    static Double cosineSimilarity(Double[] vec1, Double[] vec2) {
        if (vec1 == null || vec2 == null) {
            throw new IllegalArgumentException("向量不能为null");
        }

        if (vec1.length != vec2.length) {
            throw new IllegalArgumentException("向量维度不匹配: " + vec1.length + " vs " + vec2.length);
        }

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < vec1.length; i++) {
            dotProduct += vec1[i] * vec2[i];
            norm1 += vec1[i] * vec1[i];
            norm2 += vec2[i] * vec2[i];
        }

        double magnitude = Math.sqrt(norm1) * Math.sqrt(norm2);
        if (magnitude == 0) {
            return 0.0;
        }

        return dotProduct / magnitude;
    }

}
