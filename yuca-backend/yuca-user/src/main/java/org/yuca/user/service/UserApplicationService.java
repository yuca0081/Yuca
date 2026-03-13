package org.yuca.user.service;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.yuca.user.dto.internal.LoginResultDTO;
import org.yuca.user.dto.internal.TokenDTO;
import org.yuca.user.dto.internal.UserDTO;
import org.yuca.user.dto.request.UpdateProfileRequest;
import org.yuca.user.entity.LoginType;
import org.yuca.user.entity.RefreshToken;
import org.yuca.user.entity.User;
import org.yuca.infrastructure.security.CryptoUtils;
import org.yuca.infrastructure.storage.dto.FileInfo;
import org.yuca.infrastructure.storage.dto.UploadResult;
import org.yuca.infrastructure.storage.service.FileStorageService;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

/**
 * 用户应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserApplicationService {

    private final UserService userService;
    private final TokenService tokenService;
    private final FileStorageService fileStorageService;

    // ==================== 注册 ====================

    /**
     * 用户注册
     */
    public UserDTO register(String username, String email, String phone, String password, String nickname) {
        // 验证邮箱或手机号至少填写一个
        userService.validateEmailOrPhone(email, phone);

        // 检查唯一性
        userService.checkUsernameUnique(username);
        userService.checkEmailUnique(email);
        userService.checkPhoneUnique(phone);

        // 先进行 SHA-256 哈希，再进行 BCrypt 加密
        String sha256Hash = CryptoUtils.sha256Hex(password);
        String encodedPassword = userService.encodePassword(sha256Hash);

        // 创建用户
        User user = User.builder()
            .username(username)
            .email(email)
            .phone(phone)
            .password(encodedPassword)
            .nickname(nickname != null ? nickname : username)
            .status(1)
            .build();

        // 保存用户
        User savedUser = userService.save(user);

        return toUserDTO(savedUser);
    }

    // ==================== 登录 ====================

    /**
     * 用户登录
     */
    public LoginResultDTO login(String account, String password, Boolean rememberMe, String clientIp) {
        // 检查账号是否被锁定（登录失败次数限制）
        if (userService.isAccountLocked(account)) {
            throw new IllegalStateException("Account is locked due to too many failed login attempts");
        }

        // 识别登录类型
        LoginType loginType = userService.identifyLoginType(account);

        // 查找用户
        User user;
        try {
            user = userService.findUserByAccount(account, loginType);
        } catch (IllegalArgumentException e) {
            userService.checkLoginFailLimit(account);
            throw new IllegalArgumentException("Invalid account or password");
        }

        // 验证用户状态
        userService.validateUserForLogin(user);

        // 验证密码（支持新旧两种格式）
        // 新格式：前端发送 SHA-256(password)，数据库存储 BCrypt(SHA-256(password))
        // 旧格式：前端发送明文密码，数据库存储 BCrypt(明文密码)
        boolean passwordMatches = userService.matchesPassword(password, user.getPassword());

        // 如果直接验证失败，尝试对前端发送的哈希值再哈希一次（兼容旧用户）
        if (!passwordMatches) {
            String doubleHashedPassword = CryptoUtils.sha256Hex(password);
            passwordMatches = userService.matchesPassword(doubleHashedPassword, user.getPassword());
        }

        if (!passwordMatches) {
            userService.checkLoginFailLimit(account);
            throw new IllegalArgumentException("Invalid account or password");
        }

        // 登录成功，重置失败计数
        userService.resetLoginFailCount(account);

        // 更新登录信息
        user.updateLoginInfo(clientIp);
        userService.update(user);

        // 生成Token
        boolean isRememberMe = Boolean.TRUE.equals(rememberMe);
        String accessToken = tokenService.generateAccessToken(user, isRememberMe);
        String refreshToken = tokenService.generateRefreshToken(user);

        // 保存RefreshToken
        tokenService.saveRefreshToken(user.getId(), refreshToken);

        return LoginResultDTO.builder()
            .token(TokenDTO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(tokenService.getExpiresIn(isRememberMe))
                .build())
            .user(toUserDTO(user))
            .build();
    }

    // ==================== 登出 ====================

    /**
     * 用户登出
     */
    public void logout(Long userId, String token, String tokenId, String refreshToken) {
        // 将Token加入黑名单
        if (token != null) {
            tokenService.addTokenToBlacklist(token);
        }

        // 撤销RefreshToken
        if (refreshToken != null) {
            tokenService.findRefreshToken(refreshToken).ifPresent(tokenService::revokeRefreshToken);
        }
    }

    // ==================== 刷新Token ====================

    /**
     * 刷新Token
     */
    public TokenDTO refreshToken(String refreshToken) {
        // 验证RefreshToken格式
        if (!tokenService.validateToken(refreshToken)) {
            throw new IllegalArgumentException("Invalid refresh token");
        }

        // 检查是否在黑名单中
        if (tokenService.isRefreshTokenInBlacklist(refreshToken)) {
            throw new IllegalArgumentException("Refresh token has been revoked");
        }

        // 获取用户信息
        Long userId = tokenService.getUserId(refreshToken);

        // 查找RefreshToken
        RefreshToken tokenEntity = tokenService.findRefreshToken(refreshToken)
            .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        // 检查RefreshToken是否有效
        if (!tokenEntity.isValid()) {
            throw new IllegalArgumentException("Refresh token has expired");
        }

        // 查找用户
        User user = userService.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // 生成新的Token
        String newAccessToken = tokenService.generateAccessToken(user, false);
        String newRefreshToken = tokenService.generateRefreshToken(user);

        // 撤销旧的RefreshToken
        tokenService.revokeRefreshToken(tokenEntity);

        // 保存新的RefreshToken
        tokenService.saveRefreshToken(user.getId(), newRefreshToken);

        return TokenDTO.builder()
            .accessToken(newAccessToken)
            .refreshToken(newRefreshToken)
            .tokenType("Bearer")
            .expiresIn(tokenService.getExpiresIn(false))
            .build();
    }

    // ==================== 获取用户信息 ====================

    /**
     * 根据ID获取用户
     */
    public UserDTO getUserById(Long userId) {
        User user = userService.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return toUserDTO(user);
    }

    // ==================== 个人资料管理 ====================

    /**
     * 上传用户头像
     * @return objectName 存储在MinIO的对象名称
     */
    public String uploadAvatar(Long userId, MultipartFile file) {
        // 验证文件
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }

        // 验证文件类型（只允许图片）
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Only image files are allowed");
        }

        // 生成对象名称: user/avatar/{uuid}.{ext}
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String objectName = String.format("user/avatar/%s%s", uuid, extension);

        log.info("上传用户头像: userId={}, objectName={}, size={}", userId, objectName, file.getSize());

        // 上传到MinIO
        UploadResult uploadResult = fileStorageService.upload(file, objectName);

        // 存储objectName到数据库（不是完整URL）
        userService.updateAvatarUrl(userId, objectName);

        log.info("用户头像上传成功: userId={}, objectName={}", userId, objectName);

        return objectName;
    }

    /**
     * 获取用户头像（代理访问）
     */
    public void getAvatar(Long userId, HttpServletResponse response) throws IOException {
        // 查找用户
        User user = userService.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String objectName = user.getAvatarUrl();
        if (objectName == null || objectName.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        try {
            // 从MinIO下载文件
            InputStream inputStream = fileStorageService.download(objectName);

            // 获取文件信息
            FileInfo fileInfo = fileStorageService.getFileInfo(objectName);

            // 设置响应头
            response.setContentType(fileInfo.getContentType());
            response.setContentLengthLong(fileInfo.getFileSize());
            response.setHeader("Cache-Control", "public, max-age=86400"); // 缓存1天

            // 复制文件流到响应
            inputStream.transferTo(response.getOutputStream());
            response.flushBuffer();

        } catch (Exception e) {
            log.error("获取头像失败: userId={}, error={}", userId, e.getMessage(), e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 更新个人资料
     */
    public UserDTO updateProfile(Long userId, UpdateProfileRequest request) {
        log.info("更新个人资料: userId={}, nickname={}, email={}, phone={}",
                 userId, request.getNickname(), request.getEmail(), request.getPhone());

        // 更新用户资料
        userService.updateProfile(userId, request.getNickname(), request.getEmail(),
                                  request.getPhone(), request.getAvatarUrl());

        // 返回更新后的用户信息
        User user = userService.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return toUserDTO(user);
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return ".jpg";
        }
        int lastDotIndex = filename.lastIndexOf(".");
        if (lastDotIndex == -1) {
            return ".jpg";
        }
        return filename.substring(lastDotIndex);
    }

    // ==================== 转换方法 ====================

    private UserDTO toUserDTO(User user) {

        return UserDTO.builder()
            .id(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .phone(user.getPhone())
            .nickname(user.getNickname())
            .avatarUrl(user.getAvatarUrl())
            .status(user.getStatus())
            .createTime(user.getCreateTime())
            .lastLoginTime(user.getLastLoginTime())
            .build();
    }

    private List<UserDTO> toUserDTOList(List<User> users) {
        return users.stream()
            .map(this::toUserDTO)
            .toList();
    }
}
