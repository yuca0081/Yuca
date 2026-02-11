package org.yuca.yuca.note.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 笔记本响应DTO
 */
@Data
public class NoteBookResponse {

    /**
     * 笔记本ID
     */
    private Long id;

    /**
     * 笔记本名称
     */
    private String name;

    /**
     * 描述
     */
    private String description;

    /**
     * 图标
     */
    private String icon;

    /**
     * 排序序号
     */
    private Integer sortOrder;

    /**
     * 是否默认笔记本
     */
    private Boolean isDefault;

    /**
     * 主题颜色
     */
    private String color;

    /**
     * 节点数量（文件夹+文档）
     */
    private Integer itemCount;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
