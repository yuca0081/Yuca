package org.yuca.yuca.note.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新节点请求DTO（文件夹或文档）
 */
@Data
public class UpdateItemRequest {

    /**
     * 标题
     */
    @Size(max = 200, message = "标题最多200个字符")
    private String title;

    /**
     * 图标
     */
    @Size(max = 50, message = "图标最多50个字符")
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
     * 文档内容（仅文档类型）
     */
    private String content;

    /**
     * 内容类型：MARKDOWN, RICH_TEXT（仅文档类型）
     */
    private String contentType;

    /**
     * 摘要（仅文档类型）
     */
    @Size(max = 500, message = "摘要最多500个字符")
    private String summary;

    /**
     * 状态：DRAFT, PUBLISHED, ARCHIVED（仅文档类型）
     */
    private String status;

    /**
     * 字数统计（仅文档类型，通常由系统自动计算）
     */
    private Integer wordCount;
}
