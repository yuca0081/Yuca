package org.yuca.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.yuca.user.entity.LoginType;
import org.yuca.user.entity.User;
import org.yuca.user.mapper.UserMapper;
import org.yuca.infrastructure.cache.UserCacheService;

import java.util.Optional;

/**
 * 用户服务
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final UserCacheService userCacheService;
    private final BCryptPasswordEncoder passwordEncoder;


    public User save(User user) {
        userMapper.insert(user);
        return user;
    }

    public Optional<User> findById(Long id) {
        return Optional.ofNullable(userMapper.selectById(id));
    }

    public Optional<User> findByUsername(String username) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        return Optional.ofNullable(userMapper.selectOne(wrapper));
    }

    public Optional<User> findByEmail(String email) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getEmail, email);
        return Optional.ofNullable(userMapper.selectOne(wrapper));
    }

    public Optional<User> findByPhone(String phone) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getPhone, phone);
        return Optional.ofNullable(userMapper.selectOne(wrapper));
    }

    public User update(User user) {
        userMapper.updateById(user);
        return user;
    }

    // ==================== 业务查询 ====================

    /**
     * 根据账号（用户名/邮箱/手机号）查找用户
     */
    public Optional<User> findByAccount(String account) {
        Optional<User> user = findByUsername(account);
        if (user.isPresent()) return user;

        user = findByEmail(account);
        if (user.isPresent()) return user;

        return findByPhone(account);
    }

    /**
     * 识别登录类型
     */
    public LoginType identifyLoginType(String account) {
        if (account == null || account.trim().isEmpty()) {
            throw new IllegalArgumentException("Account cannot be empty");
        }
        if (account.contains("@")) {
            return LoginType.EMAIL;
        }
        if (account.matches("^1\\d{10}$")) {
            return LoginType.PHONE;
        }
        return LoginType.USERNAME;
    }

    /**
     * 根据账号查找用户
     */
    public User findUserByAccount(String account, LoginType loginType) {
        return switch (loginType) {
            case USERNAME -> findByUsername(account)
                .orElseThrow(() -> new IllegalArgumentException("User not found with username: " + account));
            case EMAIL -> findByEmail(account)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + account));
            case PHONE -> findByPhone(account)
                .orElseThrow(() -> new IllegalArgumentException("User not found with phone: " + account));
        };
    }

    // ==================== 唯一性检查 ====================

    public boolean existsByUsername(String username) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        return userMapper.selectCount(wrapper) > 0;
    }

    public boolean existsByEmail(String email) {
        if (email == null) return false;
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getEmail, email);
        return userMapper.selectCount(wrapper) > 0;
    }

    public boolean existsByPhone(String phone) {
        if (phone == null) return false;
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getPhone, phone);
        return userMapper.selectCount(wrapper) > 0;
    }

    // ==================== 密码相关 ====================

    /**
     * 加密密码
     */
    public String encodePassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    /**
     * 验证密码
     */
    public boolean matchesPassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    /**
     * 更新用户密码
     */
    public void updatePassword(Long userId, String encodedPassword) {
        User user = findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setPassword(encodedPassword);
        update(user);
    }

    // ==================== 登录安全 ====================

    /**
     * 检查账号是否被锁定
     */
    public boolean isAccountLocked(String account) {
        return userCacheService.isAccountLocked(account);
    }

    /**
     * 检查登录失败次数，超限则锁定
     */
    public void checkLoginFailLimit(String account) {
        if (userCacheService.isAccountLocked(account)) {
            throw new IllegalStateException("Account is locked due to too many failed login attempts");
        }

        userCacheService.incrementLoginFailCount(account);
        int failCount = userCacheService.getLoginFailCount(account);

        if (failCount >= 5) {
            userCacheService.lockAccount(account);
            throw new IllegalStateException("Account locked due to too many failed login attempts");
        }
    }

    /**
     * 重置登录失败计数
     */
    public void resetLoginFailCount(String account) {
        userCacheService.resetLoginFailCount(account);
    }

    // ==================== 用户验证 ====================

    /**
     * 验证用户是否可以登录
     */
    public void validateUserForLogin(User user) {
        if (!user.isEnabled()) {
            throw new IllegalStateException("User account is disabled");
        }
    }

    /**
     * 验证用户名唯一性
     */
    public void checkUsernameUnique(String username) {
        if (existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists");
        }
    }

    /**
     * 验证邮箱唯一性
     */
    public void checkEmailUnique(String email) {
        if (email != null && existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists");
        }
    }

    /**
     * 验证手机号唯一性
     */
    public void checkPhoneUnique(String phone) {
        if (phone != null && existsByPhone(phone)) {
            throw new IllegalArgumentException("Phone number already exists");
        }
    }

    /**
     * 验证邮箱或手机号至少填写一个
     */
    public void validateEmailOrPhone(String email, String phone) {
        if ((email == null || email.trim().isEmpty()) &&
            (phone == null || phone.trim().isEmpty())) {
            throw new IllegalArgumentException("Either email or phone number must be provided");
        }
    }

    // ==================== 个人资料管理 ====================

    /**
     * 更新用户昵称
     */
    public void updateNickname(Long userId, String nickname) {
        User user = findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setNickname(nickname);
        update(user);
    }

    /**
     * 更新用户头像URL
     */
    public void updateAvatarUrl(Long userId, String avatarUrl) {
        User user = findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setAvatarUrl(avatarUrl);
        update(user);
    }

    /**
     * 更新用户个人资料（昵称、邮箱、手机号和头像）
     */
    public void updateProfile(Long userId, String nickname, String email, String phone, String avatarUrl) {
        User user = findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (nickname != null && !nickname.trim().isEmpty()) {
            user.setNickname(nickname);
        }
        if (email != null && !email.trim().isEmpty()) {
            user.setEmail(email);
        }
        if (phone != null && !phone.trim().isEmpty()) {
            user.setPhone(phone);
        }
        if (avatarUrl != null && !avatarUrl.trim().isEmpty()) {
            user.setAvatarUrl(avatarUrl);
        }

        update(user);
    }
}
