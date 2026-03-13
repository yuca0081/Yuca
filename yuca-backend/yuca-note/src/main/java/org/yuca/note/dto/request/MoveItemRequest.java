package org.yuca.note.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 移动节点请求DTO
 */
@Data
public class MoveItemRequest {

    /**
     * 新的父节点ID（NULL表示移动到笔记本根目录）
     */
    @NotNull(message = "父节点ID不能为空")
    private Long parentId;

    /**
     * 新的排序序号（可选）
     */
    private Integer sortOrder;
}
