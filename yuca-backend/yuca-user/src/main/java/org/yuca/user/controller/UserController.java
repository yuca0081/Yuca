package org.yuca.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.yuca.user.dto.internal.LoginResultDTO;
import org.yuca.user.dto.internal.TokenDTO;
import org.yuca.user.dto.internal.UserDTO;
import org.yuca.user.dto.request.LoginRequest;
import org.yuca.user.dto.request.RefreshTokenRequest;
import org.yuca.user.dto.request.RegisterRequest;
import org.yuca.user.dto.request.ResetPasswordRequest;
import org.yuca.user.dto.request.UpdateProfileRequest;
import org.yuca.user.dto.response.LoginResponse;
import org.yuca.user.dto.response.TokenResponse;
import org.yuca.user.dto.response.UpdateProfileResponse;
import org.yuca.user.dto.response.UserResponse;
import org.yuca.common.annotation.SkipAuth;
import org.yuca.common.response.Result;
import org.yuca.user.service.UserApplicationService;
import org.yuca.user.service.TokenService;

import java.io.IOException;

/**
 * 用户控制器
 */
@Tag(name = "用户模块", description = "用户注册、登录、登出等接口")
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserApplicationService userApplicationService;
    private final TokenService tokenService;

    @SkipAuth
    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "使用用户名、密码注册新用户，需要提供邮箱或手机号")
    public Result<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserDTO userDTO = userApplicationService.register(
            request.getUsername(),
            request.getEmail(),
            request.getPhone(),
            request.getPassword(),
            request.getNickname()
        );
        return Result.success(toUserResponse(userDTO));
    }

    @SkipAuth
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "支持用户名、邮箱、手机号登录")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        LoginResultDTO result = userApplicationService.login(
            request.getAccount(),
            request.getPassword(),
            request.getRememberMe(),
            getClientIp(httpRequest)
        );
        return Result.success(toLoginResponse(result));
    }

    @PostMapping("/logout")
    @Operation(summary = "退出登录", description = "退出登录并清除Token")
    public Result<Void> logout(HttpServletRequest request) {
        String token = extractToken(request);
        Long userId = getCurrentUserId(request);
        String tokenId = getTokenId(request);
        String refreshToken = request.getHeader("X-Refresh-Token");

        userApplicationService.logout(userId, token, tokenId, refreshToken);
        return Result.success("Logout successful", null);
    }

    @SkipAuth
    @PostMapping("/refresh-token")
    @Operation(summary = "刷新令牌", description = "使用刷新令牌获取新的访问令牌")
    public Result<TokenResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        TokenDTO tokenDTO = userApplicationService.refreshToken(request.getRefreshToken());
        return Result.success(toTokenResponse(tokenDTO));
    }

    @SkipAuth
    @PostMapping("/reset-password")
    @Operation(summary = "重置密码", description = "通过账号重置密码，无需验证")
    public Result<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        userApplicationService.resetPassword(request.getAccount(), request.getNewPassword());
        return Result.success("Password reset successful", null);
    }

    @GetMapping("/current")
    @Operation(summary = "获取当前用户信息", description = "获取当前登录用户的信息")
    public Result<UserResponse> getCurrentUser(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        UserDTO userDTO = userApplicationService.getUserById(userId);
        return Result.success(toUserResponse(userDTO));
    }

    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传用户头像", description = "上传当前用户的头像图片，支持jpg、png等图片格式")
    public Result<Void> uploadAvatar(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        userApplicationService.uploadAvatar(userId, file);
        return Result.success("头像上传成功", null);
    }

    @SkipAuth
    @GetMapping("/avatar/{userId}")
    @Operation(summary = "获取用户头像", description = "通过用户ID获取头像图片")
    public void getAvatar(
            @PathVariable Long userId,
            HttpServletResponse response) throws IOException {
        userApplicationService.getAvatar(userId, response);
    }

    @PutMapping("/profile")
    @Operation(summary = "更新个人资料", description = "更新当前用户的昵称和头像URL")
    public Result<UpdateProfileResponse> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            HttpServletRequest httpRequest) {
        Long userId = getCurrentUserId(httpRequest);
        UserDTO userDTO = userApplicationService.updateProfile(userId, request);
        return Result.success(toUpdateProfileResponse(userDTO));
    }


    /**
     * 从请求头中提取Bearer令牌
     */
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /**
     * 从请求头中提取当前用户ID
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        String token = extractToken(request);
        if (token != null) {
            return tokenService.getUserId(token);
        }
        return null;
    }

    /**
     * 从请求头中提取Token ID
     */
    private String getTokenId(HttpServletRequest request) {
        String token = extractToken(request);
        if (token != null) {
            return tokenService.getTokenId(token);
        }
        return null;
    }
    /**
     * 从请求头中提取客户端IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
    /**
     * 将UserDTO转换为UserResponse
     */
    private UserResponse toUserResponse(UserDTO dto) {
        return UserResponse.builder()
            .id(dto.getId())
            .username(dto.getUsername())
            .email(dto.getEmail())
            .phone(dto.getPhone())
            .nickname(dto.getNickname())
            .avatarUrl(dto.getAvatarUrl())
            .status(dto.getStatus())
            .createTime(dto.getCreateTime())
            .lastLoginTime(dto.getLastLoginTime())
            .build();
    }

    /**
     * 将LoginResultDTO转换为LoginResponse
     */
    private LoginResponse toLoginResponse(LoginResultDTO dto) {
        return LoginResponse.builder()
            .accessToken(dto.getToken().getAccessToken())
            .refreshToken(dto.getToken().getRefreshToken())
            .tokenType(dto.getToken().getTokenType())
            .expiresIn(dto.getToken().getExpiresIn())
            .user(toUserResponse(dto.getUser()))
            .build();
    }

    /**
     * 将TokenDTO转换为TokenResponse
     */
    private TokenResponse toTokenResponse(TokenDTO dto) {
        return TokenResponse.builder()
            .accessToken(dto.getAccessToken())
            .refreshToken(dto.getRefreshToken())
            .tokenType(dto.getTokenType())
            .expiresIn(dto.getExpiresIn())
            .build();
    }

    /**
     * 将UserDTO转换为UpdateProfileResponse
     */
    private UpdateProfileResponse toUpdateProfileResponse(UserDTO dto) {
        return UpdateProfileResponse.builder()
            .id(dto.getId())
            .username(dto.getUsername())
            .email(dto.getEmail())
            .phone(dto.getPhone())
            .nickname(dto.getNickname())
            .avatarUrl(dto.getAvatarUrl())
            .status(dto.getStatus())
            .createTime(dto.getCreateTime() != null ? dto.getCreateTime().toString() : null)
            .lastLoginTime(dto.getLastLoginTime() != null ? dto.getLastLoginTime().toString() : null)
            .build();
    }
}
