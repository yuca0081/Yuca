package org.yuca.note.dto.request;

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
     * 同级排序序号
     */
    private Integer sortOrder;

    // ========== 文档专用字段 ==========

    /**
     * 文档内容（仅文档类型）
     */
    private String content;

    /**
     * 状态：DRAFT, PUBLISHED, ARCHIVED（仅文档类型）
     */
    private String status;

    /**
     * 字数统计（仅文档类型，通常由系统自动计算）
     */
    private Integer wordCount;
}
