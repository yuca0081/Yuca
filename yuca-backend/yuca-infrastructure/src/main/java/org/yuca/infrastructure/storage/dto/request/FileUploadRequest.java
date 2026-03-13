package org.yuca.infrastructure.storage.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 文件上传请求DTO
 *
 * @author Yuca
 * @since 2025-01-29
 */
@Data
@Schema(description = "文件上传请求")
public class FileUploadRequest {

    @Schema(description = "来源类型", example = "user", required = true)
    private String sourceType;

    @Schema(description = "业务类型", example = "knowledge")
    private String businessType;

    @Schema(description = "业务关联ID", example = "1")
    private Long businessId;

    @Schema(description = "扩展元数据（JSON字符串）")
    private String metadata;
}
