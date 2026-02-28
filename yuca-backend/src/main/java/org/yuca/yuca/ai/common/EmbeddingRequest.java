package org.yuca.yuca.ai.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * AI 向量嵌入请求
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
     * 输入文本列表
     */
    private List<String> inputs;

    /**
     * 模型名称（可选，默认使用配置的模型）
     */
    private String model;

    /**
     * 嵌入维度（可选）
     */
    private Integer dimensions;
}
