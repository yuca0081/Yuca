package org.yuca.note.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建笔记本请求DTO
 */
@Data
public class CreateNoteBookRequest {

    /**
     * 笔记本名称
     */
    @NotBlank(message = "笔记本名称不能为空")
    @Size(max = 100, message = "笔记本名称最多100个字符")
    private String name;

    /**
     * 描述
     */
    @Size(max = 500, message = "描述最多500个字符")
    private String description;

    /**
     * 图标
     */
    @Size(max = 50, message = "图标最多50个字符")
    private String icon;

    /**
     * 主题颜色
     */
    @Size(max = 20, message = "颜色最多20个字符")
    private String color;
}
