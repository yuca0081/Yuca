package org.yuca.ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Embedding请求（兼容OpenAI格式）
 *
 * @author Yuca
 * @since 2025-01-27
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmbeddingRequest {

    /**
     * 输入文本列表（OpenAI格式用input）
     */
    private List<String> inputs;

    /**
     * 模型名称（可选，覆盖配置的默认模型）
     */
    private String model;

    /**
     * 向量维度（可选，某些千问模型支持）
     */
    private Integer dimensions;
}
