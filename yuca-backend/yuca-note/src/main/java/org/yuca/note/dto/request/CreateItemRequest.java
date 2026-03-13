package org.yuca.note.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建节点请求DTO（文件夹或文档）
 */
@Data
public class CreateItemRequest {

    /**
     * 所属笔记本ID
     */
    @NotNull(message = "笔记本ID不能为空")
    private Long bookId;

    /**
     * 父节点ID（可选，NULL表示笔记本根目录）
     */
    private Long parentId;

    /**
     * 节点类型：FOLDER, DOCUMENT
     */
    @NotBlank(message = "节点类型不能为空")
    private String type;

    /**
     * 标题
     */
    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题最多200个字符")
    private String title;

    /**
     * 图标
     */
    @Size(max = 50, message = "图标最多50个字符")
    private String icon;

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
}
