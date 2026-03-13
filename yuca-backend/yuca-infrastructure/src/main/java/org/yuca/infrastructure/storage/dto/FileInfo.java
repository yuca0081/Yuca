package org.yuca.infrastructure.storage.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 文件信息DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileInfo {

    /**
     * 对象名称（存储路径）
     */
    private String objectName;

    /**
     * 文件名称
     */
    private String fileName;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 内容类型
     */
    private String contentType;

    /**
     * ETag
     */
    private String etag;

    /**
     * 最后修改时间
     */
    private LocalDateTime lastModified;
}
