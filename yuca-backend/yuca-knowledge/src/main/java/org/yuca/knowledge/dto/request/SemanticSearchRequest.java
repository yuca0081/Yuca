package org.yuca.knowledge.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 语义搜索请求DTO
 *
 * @author Yuca
 * @since 2025-01-27
 */
@Data
public class SemanticSearchRequest {

    /**
     * 知识库ID
     */
    @NotNull(message = "知识库ID不能为空")
    private Long kbId;

    /**
     * 搜索查询文本
     */
    @NotBlank(message = "查询文本不能为空")
    private String query;

    /**
     * 返回结果数量（默认5）
     */
    private Integer topK = 5;

    /**
     * 相似度阈值（0-1，默认0.7）
     */
    private Double threshold = 0.7;
}
