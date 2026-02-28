package org.yuca.yuca.ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * AI 向量嵌入响应
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
     * 嵌入结果列表
     */
    private List<EmbeddingResult> results;

    /**
     * 使用的模型
     */
    private String model;

    /**
     * Token 使用情况（可选）
     */
    private Usage usage;

    /**
     * 单个嵌入结果
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmbeddingResult {

        /**
         * 向量数组（使用Double保持精度）
         */
        private Double[] embedding;

        /**
         * 索引
         */
        private Integer index;
    }
}
