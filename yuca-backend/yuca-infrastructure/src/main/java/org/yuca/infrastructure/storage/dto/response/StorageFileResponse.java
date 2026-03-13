package org.yuca.infrastructure.storage.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 文件存储记录响应DTO
 *
 * @author Yuca
 * @since 2025-01-29
 */
@Data
@Schema(description = "文件存储记录信息")
public class StorageFileResponse {

    @Schema(description = "文件ID")
    private Long id;

    @Schema(description = "上传者ID")
    private Long uploadedBy;

    @Schema(description = "文件名")
    private String fileName;

    @Schema(description = "文件大小（字节）")
    private Long fileSize;

    @Schema(description = "MIME类型")
    private String contentType;

    @Schema(description = "文件URL")
    private String fileUrl;

    @Schema(description = "文件分类")
    private String fileType;

    @Schema(description = "来源类型")
    private String sourceType;

    @Schema(description = "业务类型")
    private String businessType;

    @Schema(description = "业务关联ID")
    private Long businessId;

    @Schema(description = "扩展元数据")
    private String metadata;

    @Schema(description = "上传时间")
    private LocalDateTime createdAt;
}
