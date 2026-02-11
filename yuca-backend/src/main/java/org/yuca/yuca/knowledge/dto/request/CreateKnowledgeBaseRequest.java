package org.yuca.yuca.knowledge.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建知识库请求DTO
 *
 * @author Yuca
 * @since 2025-01-27
 */
@Data
public class CreateKnowledgeBaseRequest {

    /**
     * 知识库名称
     */
    @NotBlank(message = "知识库名称不能为空")
    @Size(max = 100, message = "知识库名称最多100个字符")
    private String name;

    /**
     * 知识库描述
     */
    private String description;
}
