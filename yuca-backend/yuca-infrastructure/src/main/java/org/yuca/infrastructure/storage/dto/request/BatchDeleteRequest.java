package org.yuca.infrastructure.storage.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 批量删除文件请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "批量删除文件请求")
public class BatchDeleteRequest {

    @NotEmpty(message = "文件路径列表不能为空")
    @Schema(description = "文件对象名称列表", example = "[\"image/2025/01/27/xxx.jpg\"]")
    private List<String> objectNames;
}
