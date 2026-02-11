package org.yuca.yuca.note.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新笔记本请求DTO
 */
@Data
public class UpdateNoteBookRequest {


    private Long id;
    /**
     * 笔记本名称
     */
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
