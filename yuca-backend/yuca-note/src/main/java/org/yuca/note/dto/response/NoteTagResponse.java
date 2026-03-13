package org.yuca.note.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 标签响应DTO
 */
@Data
public class NoteTagResponse {

    /**
     * 标签ID
     */
    private Long id;

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
    private LocalDateTime createdAt;
}
