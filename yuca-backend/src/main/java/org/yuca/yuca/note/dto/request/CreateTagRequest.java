package org.yuca.yuca.note.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建标签请求DTO
 */
@Data
public class CreateTagRequest {

    /**
     * 标签名称
     */
    @NotBlank(message = "标签名称不能为空")
    @Size(max = 50, message = "标签名称最多50个字符")
    private String name;

    /**
     * 标签颜色
     */
    @Size(max = 20, message = "颜色最多20个字符")
    private String color;
}
