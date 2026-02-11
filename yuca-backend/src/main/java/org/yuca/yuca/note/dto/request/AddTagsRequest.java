package org.yuca.yuca.note.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 给文档添加标签请求DTO
 */
@Data
public class AddTagsRequest {

    /**
     * 标签ID列表
     */
    @NotNull(message = "标签ID列表不能为空")
    private List<Long> tagIds;
}
