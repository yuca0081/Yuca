package org.yuca.ai.client.openai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * OpenAI Embedding API 请求
 *
 * @author Yuca
 * @since 2025-01-27
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpenAIEmbeddingRequest {

    /**
     * 输入文本（单个文本或文本列表）
     */
    private Object input;

    /**
     * 模型名称
     */
    private String model;

    /**
     * 嵌入维度（可选）
     */
    private Integer dimensions;

    /**
     * 嵌入格式（可选）
     */
    private String encodingFormat;
}
