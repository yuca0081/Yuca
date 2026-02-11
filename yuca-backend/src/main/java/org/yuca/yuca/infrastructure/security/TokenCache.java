package org.yuca.yuca.security;

/**
 * Token缓存接口
 * 用于JWT认证过滤器的Token缓存操作
 */
public interface TokenCache {

    /**
     * 检查Token是否在黑名单中
     */
    boolean isTokenInBlacklist(String token);

    /**
     * 检查Token是否在白名单中
     */
    boolean isTokenInWhitelist(Long userId, String tokenId);

    /**
     * 将Token加入黑名单
     */
    void addToBlacklist(String token);

    /**
     * 将Token加入白名单
     */
    void addToWhitelist(Long userId, String tokenId);

    /**
     * 从白名单中移除Token
     */
    void removeFromWhitelist(Long userId, String tokenId);
}
