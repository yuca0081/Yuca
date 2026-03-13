package org.yuca.note.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 节点响应DTO（文件夹或文档）
 */
@Data
public class NoteItemResponse {

    /**
     * 节点ID
     */
    private Long id;

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

    // ========== 通用字段 ==========

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

    // ========== 文档专用字段 ==========

    /**
     * 文档内容（列表查询时不返回）
     */
    private String content;

    /**
     * 内容类型：MARKDOWN, RICH_TEXT
     */
    private String contentType;

    /**
     * 摘要
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

    /**
     * 标签列表（仅文档）
     */
    private List<NoteTagResponse> tags;

    // ========== 文件夹统计字段 ==========

    /**
     * 直接子项数量
     */
    private Integer childCount;

    // ========== 时间字段 ==========

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 发布时间
     */
    private LocalDateTime publishedAt;
}
