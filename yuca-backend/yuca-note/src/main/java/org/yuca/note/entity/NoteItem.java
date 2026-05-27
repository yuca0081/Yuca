package org.yuca.note.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.yuca.note.enums.DocumentStatus;
import org.yuca.note.enums.ItemType;

import java.time.LocalDateTime;

/**
 * 节点实体（单表设计：文件夹和文档统一存储）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("note_item")
public class NoteItem {

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
     * 所属笔记本ID
     */
    private Long bookId;

    /**
     * 父节点ID（NULL表示笔记本根目录）
     */
    private Long parentId;

    /**
     * 节点类型：FOLDER, DOCUMENT
     */
    private String type;

    // ========== 通用字段（文件夹和文档都有） ==========

    /**
     * 标题
     */
    private String title;

    /**
     * 同级排序序号
     */
    private Integer sortOrder;

    // ========== 文档专用字段（FOLDER类型时为NULL） ==========

    /**
     * 文档内容
     */
    private String content;

    /**
     * 状态：DRAFT, PUBLISHED, ARCHIVED
     */
    private String status;

    /**
     * 字数统计
     */
    private Integer wordCount;

    // ========== 文件夹统计字段（DOCUMENT类型时为NULL） ==========

    /**
     * 直接子项数量
     */
    private Integer childCount;

    // ========== 通用时间字段 ==========

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
     * 发布时间
     */
    private LocalDateTime publishedAt;

    /**
     * 逻辑删除标记：0-正常，1-已删除
     */
    @TableLogic
    private Integer deleted;

    // ========== 便捷方法 ==========

    /**
     * 检查是否为文件夹
     */
    public boolean isFolder() {
        return ItemType.FOLDER.getCode().equals(type);
    }

    /**
     * 检查是否为文档
     */
    public boolean isDocument() {
        return ItemType.DOCUMENT.getCode().equals(type);
    }

    /**
     * 检查是否为草稿
     */
    public boolean isDraft() {
        return DocumentStatus.DRAFT.getCode().equals(status);
    }

    /**
     * 检查是否已发布
     */
    public boolean isPublished() {
        return DocumentStatus.PUBLISHED.getCode().equals(status);
    }

    /**
     * 设置为文件夹
     */
    public void setAsFolder() {
        this.type = ItemType.FOLDER.getCode();
        this.content = null;
    }

    /**
     * 设置为文档
     */
    public void setAsDocument() {
        this.type = ItemType.DOCUMENT.getCode();
    }

    /**
     * 增加子项计数（仅文件夹）
     */
    public void incrementChildCount() {
        if (!isFolder()) {
            return;
        }
        if (this.childCount == null) {
            this.childCount = 0;
        }
        this.childCount++;
    }

    /**
     * 减少子项计数（仅文件夹）
     */
    public void decrementChildCount() {
        if (!isFolder()) {
            return;
        }
        if (this.childCount == null || this.childCount <= 0) {
            this.childCount = 0;
        } else {
            this.childCount--;
        }
    }
}
