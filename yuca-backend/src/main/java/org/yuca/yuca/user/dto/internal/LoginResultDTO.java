package org.yuca.yuca.user.dto.internal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录结果DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResultDTO {

    /**
     * Token信息
     */
    private TokenDTO token;

    /**
     * 用户信息
     */
    private UserDTO user;
}
