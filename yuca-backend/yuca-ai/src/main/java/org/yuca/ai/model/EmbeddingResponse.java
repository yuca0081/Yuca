package org.yuca.ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Embedding响应（兼容OpenAI格式）
 *
 * @author Yuca
 * @since 2025-01-27
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmbeddingResponse {

    /**
     * 对象类型
     */
    private String object;

    /**
     * 模型名称
     */
    private String model;

    /**
     * 嵌入结果列表
     */
    private List<EmbeddingData> data;

    /**
     * Token使用统计
     */
    private BaseChatResponse.Usage usage;

    // ========== 充血方法 ==========

    /**
     * 获取第一个嵌入向量（便捷方法，用于单条嵌入）
     */
    public List<Double> getSingleEmbedding() {
        if (data == null || data.isEmpty()) {
            return null;
        }
        return data.getFirst().getEmbedding();
    }

    /**
     * 获取第一个嵌入向量数组形式（便捷方法）
     */
    public Double[] getEmbeddingArray() {
        List<Double> embedding = getSingleEmbedding();
        if (embedding == null) {
            return null;
        }
        return embedding.toArray(new Double[0]);
    }

    /**
     * 嵌入数据
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmbeddingData {
        /**
         * 索引
         */
        private Integer index;

        /**
         * 嵌入向量
         */
        private List<Double> embedding;

        /**
         * 对象类型
         */
        private String object;
    }
}
