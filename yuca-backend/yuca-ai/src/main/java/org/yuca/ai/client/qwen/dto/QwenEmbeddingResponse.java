package org.yuca.ai.client.qwen.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 千问 API Embedding 响应格式（兼容 OpenAI）
 *
 * @author Yuca
 * @since 2025-01-27
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QwenEmbeddingResponse {

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
     * Token 使用情况
     */
    private Usage usage;

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
         * 嵌入向量（数组）
         */
        private List<Double> embedding;

        /**
         * 对象类型
         */
        private String object;
    }

    /**
     * 使用情况
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Usage {
        /**
         * 输入 token 数
         */
        private Integer prompt_tokens;

        /**
         * 总 token 数
         */
        private Integer total_tokens;
    }
}
