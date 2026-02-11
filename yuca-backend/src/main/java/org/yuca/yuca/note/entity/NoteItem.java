package org.yuca.yuca.note.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.yuca.yuca.note.enums.DocumentType;
import org.yuca.yuca.note.enums.DocumentStatus;
import org.yuca.yuca.note.enums.ItemType;

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
     * 图标
     */
    private String icon;

    /**
     * 同级排序序号
     */
    private Integer sortOrder;

    /**
     * 是否置顶
     */
    private Boolean isPinned;

    // ========== 文档专用字段（FOLDER类型时为NULL） ==========

    /**
     * 文档内容
     */
    private String content;

    /**
     * 内容类型：MARKDOWN, RICH_TEXT
     */
    private String contentType;

    /**
     * 摘要（前500字）
     */
    private String summary;

    /**
     * 状态：DRAFT, PUBLISHED, ARCHIVED
     */
    private String status;

    /**
     * 浏览次数
     */
    private Integer viewCount;

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
     * 检查是否为Markdown格式
     */
    public boolean isMarkdown() {
        return DocumentType.MARKDOWN.getCode().equals(contentType);
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
     * 检查是否置顶
     */
    public boolean isPinnedItem() {
        return Boolean.TRUE.equals(isPinned);
    }

    /**
     * 设置为文件夹
     */
    public void setAsFolder() {
        this.type = ItemType.FOLDER.getCode();
        this.content = null;
        this.contentType = null;
    }

    /**
     * 设置为文档
     */
    public void setAsDocument() {
        this.type = ItemType.DOCUMENT.getCode();
    }

    /**
     * 增加浏览次数
     */
    public void incrementViewCount() {
        if (this.viewCount == null) {
            this.viewCount = 0;
        }
        this.viewCount++;
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
