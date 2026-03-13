package org.yuca.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 刷新令牌请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "刷新令牌请求")
public class RefreshTokenRequest {

    @Schema(description = "刷新令牌", required = true)
    @NotBlank(message = "Refresh token cannot be empty")
    private String refreshToken;
}
