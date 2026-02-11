package org.yuca.yuca.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 发送验证码请求DTO（预留）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "发送验证码请求")
public class SendCodeRequest {

    @Schema(description = "手机号", required = true, example = "13800138000")
    @NotBlank(message = "Phone number cannot be empty")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "Invalid phone number format")
    private String phone;

    @Schema(description = "验证码类型：REGISTER-注册, LOGIN-登录, RESET_PASSWORD-重置密码", required = true, example = "LOGIN")
    @NotBlank(message = "Code type cannot be empty")
    private String codeType;
}
