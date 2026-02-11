package org.yuca.yuca.knowledge.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识库响应DTO
 *
 * @author Yuca
 * @since 2025-01-27
 */
@Data
public class KnowledgeBaseResponse {

    /**
     * 知识库ID
     */
    private Long id;

    /**
     * 所属用户ID
     */
    private Long userId;

    /**
     * 知识库名称
     */
    private String name;

    /**
     * 知识库描述
     */
    private String description;

    /**
     * 文档数量
     */
    private Integer docCount;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
