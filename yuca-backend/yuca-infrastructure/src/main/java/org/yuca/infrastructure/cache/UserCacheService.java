package org.yuca.infrastructure.cache;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 用户缓存服务
 */
@Service
public class UserCacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${login.lock-duration}")
    private long lockDuration;

    private static final String USER_INFO_PREFIX = "user:info:";
    private static final String TOKEN_BLACKLIST_PREFIX = "token:blacklist:";
    private static final String REFRESH_TOKEN_BLACKLIST_PREFIX = "refresh-token:blacklist:";
    private static final String LOGIN_FAIL_PREFIX = "login:fail:";
    private static final String LOGIN_LOCKED_PREFIX = "login:locked:";

    public UserCacheService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }



    /**
     * 将令牌加入黑名单
     */
    public void addTokenToBlacklist(String token, long ttl) {
        String key = TOKEN_BLACKLIST_PREFIX + token;
        redisTemplate.opsForValue().set(key, "1", ttl, TimeUnit.MILLISECONDS);
    }

    /**
     * 检查令牌是否在黑名单中
     */
    public boolean isTokenInBlacklist(String token) {
        String key = TOKEN_BLACKLIST_PREFIX + token;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * 将刷新令牌加入黑名单
     */
    public void addRefreshTokenToBlacklist(String token, long ttl) {
        String key = REFRESH_TOKEN_BLACKLIST_PREFIX + token;
        redisTemplate.opsForValue().set(key, "1", ttl, TimeUnit.MILLISECONDS);
    }

    /**
     * 检查刷新令牌是否在黑名单中
     */
    public boolean isRefreshTokenInBlacklist(String token) {
        String key = REFRESH_TOKEN_BLACKLIST_PREFIX + token;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    // ==================== 用户信息缓存 ====================

    /**
     * 缓存用户信息
     */
    public void cacheUserInfo(Long userId, Object userInfo) {
        String key = USER_INFO_PREFIX + userId;
        redisTemplate.opsForValue().set(key, userInfo, 1, TimeUnit.HOURS);
    }

    /**
     * 获取缓存的用户信息
     */
    public Object getCachedUserInfo(Long userId) {
        String key = USER_INFO_PREFIX + userId;
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 删除用户信息缓存
     */
    public void removeUserInfoCache(Long userId) {
        String key = USER_INFO_PREFIX + userId;
        redisTemplate.delete(key);
    }

    // ==================== 登录失败计数 ====================

    /**
     * 增加登录失败次数
     */
    public void incrementLoginFailCount(String account) {
        String key = LOGIN_FAIL_PREFIX + account;
        redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, Duration.ofMillis(lockDuration));
    }

    /**
     * 获取登录失败次数
     */
    public int getLoginFailCount(String account) {
        String key = LOGIN_FAIL_PREFIX + account;
        Object count = redisTemplate.opsForValue().get(key);
        return count != null ? (Integer) count : 0;
    }

    /**
     * 重置登录失败次数
     */
    public void resetLoginFailCount(String account) {
        String key = LOGIN_FAIL_PREFIX + account;
        redisTemplate.delete(key);
    }

    /**
     * 锁定账号
     */
    public void lockAccount(String account) {
        String key = LOGIN_LOCKED_PREFIX + account;
        redisTemplate.opsForValue().set(key, System.currentTimeMillis(), lockDuration, TimeUnit.MILLISECONDS);
    }

    /**
     * 检查账号是否被锁定
     */
    public boolean isAccountLocked(String account) {
        String key = LOGIN_LOCKED_PREFIX + account;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * 解锁账号
     */
    public void unlockAccount(String account) {
        String key = LOGIN_LOCKED_PREFIX + account;
        redisTemplate.delete(key);
    }

}