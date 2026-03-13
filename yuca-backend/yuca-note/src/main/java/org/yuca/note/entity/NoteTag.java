package org.yuca.note.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 标签实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("note_tag")
public class NoteTag {

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
     * 标签名称
     */
    private String name;

    /**
     * 标签颜色
     */
    private String color;

    /**
     * 使用次数
     */
    private Integer useCount;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 逻辑删除标记：0-正常，1-已删除
     */
    @TableLogic
    private Integer deleted;

    /**
     * 增加使用次数
     */
    public void incrementUseCount() {
        if (this.useCount == null) {
            this.useCount = 0;
        }
        this.useCount++;
    }

    /**
     * 减少使用次数
     */
    public void decrementUseCount() {
        if (this.useCount == null || this.useCount <= 0) {
            this.useCount = 0;
        } else {
            this.useCount--;
        }
    }
}
