package org.yuca.infrastructure.storage.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件上传结果DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadResult {

    /**
     * 对象名称（存储路径）
     */
    private String objectName;

    /**
     * 原始文件名
     */
    private String originalFileName;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 内容类型
     */
    private String contentType;

    /**
     * 访问URL
     */
    private String url;

    /**
     * ETag
     */
    private String etag;
}
