package org.yuca.yuca.note.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 笔记本实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("note_book")
public class NoteBook {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属用户ID
     */
    private Long userId;

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
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 逻辑删除标记：0-正常，1-已删除
     */
    @TableLogic
    private Integer deleted;

    /**
     * 检查是否为默认笔记本
     */
    public boolean isDefaultBook() {
        return Boolean.TRUE.equals(isDefault);
    }

    /**
     * 设置为默认笔记本
     */
    public void setAsDefault() {
        this.isDefault = true;
    }

    /**
     * 增加节点计数
     */
    public void incrementItemCount() {
        if (this.itemCount == null) {
            this.itemCount = 0;
        }
        this.itemCount++;
    }

    /**
     * 减少节点计数
     */
    public void decrementItemCount() {
        if (this.itemCount == null || this.itemCount <= 0) {
            this.itemCount = 0;
        } else {
            this.itemCount--;
        }
    }
}
