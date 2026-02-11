package org.yuca.yuca.note.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新标签请求DTO
 */
@Data
public class UpdateTagRequest {

    /**
     * 标签ID
     */
    private Long id;

    /**
     * 标签名称
     */
    @Size(max = 50, message = "标签名称最多50个字符")
    private String name;

    /**
     * 标签颜色
     */
    @Size(max = 20, message = "颜色最多20个字符")
    private String color;
}
