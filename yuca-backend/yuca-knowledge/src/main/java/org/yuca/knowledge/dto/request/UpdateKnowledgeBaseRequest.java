package org.yuca.knowledge.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 更新知识库请求DTO
 *
 * @author Yuca
 * @since 2025-01-27
 */
@Data
public class UpdateKnowledgeBaseRequest {

    /**
     * 知识库ID
     */
    @NotNull(message = "知识库ID不能为空")
    private Long id;

    /**
     * 知识库名称
     */
    private String name;

    /**
     * 知识库描述
     */
    private String description;
}
