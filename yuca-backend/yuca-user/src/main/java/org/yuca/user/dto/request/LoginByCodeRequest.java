package org.yuca.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 验证码登录请求DTO（预留）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "验证码登录请求")
public class LoginByCodeRequest {

    @Schema(description = "手机号", required = true, example = "13800138000")
    @NotBlank(message = "Phone number cannot be empty")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "Invalid phone number format")
    private String phone;

    @Schema(description = "验证码", required = true, example = "123456")
    @NotBlank(message = "Verification code cannot be empty")
    private String code;

    @Schema(description = "是否记住我", example = "false")
    private Boolean rememberMe;
}
