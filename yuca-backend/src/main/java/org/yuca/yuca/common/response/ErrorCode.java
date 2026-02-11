package org.yuca.yuca.common.response;

import lombok.Getter;

/**
 * 错误码枚举
 */
@Getter
public enum ErrorCode {

    // 通用错误 1000-1999
    SUCCESS(200, "Success"),
    BAD_REQUEST(400, "Bad Request"),
    UNAUTHORIZED(401, "Unauthorized"),
    FORBIDDEN(403, "Forbidden"),
    NOT_FOUND(404, "Not Found"),
    PARAM_ERROR(1000, "Parameter error"),
    SYSTEM_ERROR(1001, "System error"),
    INTERNAL_SERVER_ERROR(500, "Internal Server Error"),

    // 用户相关错误 2000-2999
    USER_NOT_FOUND(2001, "User not found"),
    USERNAME_EXISTS(2002, "Username already exists"),
    EMAIL_EXISTS(2003, "Email already exists"),
    PHONE_EXISTS(2004, "Phone number already exists"),
    PASSWORD_ERROR(2005, "Incorrect password"),
    PASSWORD_FORMAT_ERROR(2006, "Password format error"),
    USER_DISABLED(2007, "User account is disabled"),
    USER_LOCKED(2008, "User account is locked"),
    LOGIN_FAIL_EXCEED_LIMIT(2009, "Login failed too many times, account is locked"),

    // Token相关错误 3000-3999
    TOKEN_INVALID(3001, "Invalid token"),
    TOKEN_EXPIRED(3002, "Token has expired"),
    REFRESH_TOKEN_INVALID(3003, "Invalid refresh token"),
    REFRESH_TOKEN_EXPIRED(3004, "Refresh token has expired"),
    REFRESH_TOKEN_REVOKED(3005, "Refresh token has been revoked"),

    // 验证码相关错误 4000-4999
    VERIFICATION_CODE_INVALID(4001, "Invalid verification code"),
    VERIFICATION_CODE_EXPIRED(4002, "Verification code has expired"),
    VERIFICATION_CODE_USED(4003, "Verification code has been used"),
    SEND_CODE_TOO_FREQUENT(4004, "Send verification code too frequently"),
    ;

    private final Integer code;
    private final String message;

    ErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
