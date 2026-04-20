package org.yuca.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 重置密码请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "重置密码请求")
public class ResetPasswordRequest {

    @Schema(description = "账号（用户名/邮箱/手机号）", required = true, example = "zhangsan")
    @NotBlank(message = "Account cannot be empty")
    private String account;

    @Schema(description = "新密码", required = true, example = "NewPass123")
    @NotBlank(message = "New password cannot be empty")
    private String newPassword;
}
