package org.yuca.note.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 文档版本历史实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("note_version")
public class NoteVersion {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 节点ID（仅文档类型）
     */
    private Long itemId;

    /**
     * 版本号
     */
    private Integer versionNumber;

    /**
     * 标题
     */
    private String title;

    /**
     * 内容
     */
    private String content;

    /**
     * 变更说明
     */
    private String changeNote;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
