package org.yuca.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户登录请求")
public class LoginRequest {

    @Schema(description = "账号（用户名/邮箱/手机号）", required = true, example = "zhangsan")
    @NotBlank(message = "Account cannot be empty")
    private String account;

    @Schema(description = "密码", required = true, example = "Pass123")
    @NotBlank(message = "Password cannot be empty")
    private String password;

    @Schema(description = "是否记住我", example = "false")
    private Boolean rememberMe;
}
