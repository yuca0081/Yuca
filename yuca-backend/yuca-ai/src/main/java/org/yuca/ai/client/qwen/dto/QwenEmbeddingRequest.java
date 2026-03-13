package org.yuca.ai.client.qwen.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 千问 API Embedding 请求格式（兼容 OpenAI）
 *
 * @author Yuca
 * @since 2025-01-27
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QwenEmbeddingRequest {

    /**
     * 输入文本列表
     */
    private List<String> input;

    /**
     * 模型名称
     */
    private String model;

    /**
     * 向量维度
     */
    private Integer dimensions;
}
