package org.yuca.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.yuca.user.entity.RefreshToken;
import org.yuca.user.entity.User;
import org.yuca.user.mapper.RefreshTokenMapper;
import org.yuca.infrastructure.cache.UserCacheService;
import org.yuca.infrastructure.security.JwtTokenProvider;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Token服务
 */
@Service
@RequiredArgsConstructor
public class TokenService {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenMapper refreshTokenMapper;
    private final UserCacheService userCacheService;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.remember-me-expiration}")
    private long rememberMeExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    // ==================== Token生成 ====================

    /**
     * 生成访问令牌
     */
    public String generateAccessToken(User user, boolean rememberMe) {
        return jwtTokenProvider.generateAccessToken(user.getId(), user.getUsername(), rememberMe);
    }

    /**
     * 生成刷新令牌
     */
    public String generateRefreshToken(User user) {
        return jwtTokenProvider.generateRefreshToken(user.getId(), user.getUsername());
    }

    /**
     * 获取Token过期时间（秒）
     */
    public long getExpiresIn(boolean rememberMe) {
        return rememberMe ? rememberMeExpiration / 1000 : accessTokenExpiration / 1000;
    }

    // ==================== Token验证 ====================

    /**
     * 验证Token
     */
    public boolean validateToken(String token) {
        if (!jwtTokenProvider.validateToken(token)) {
            return false;
        }
        if (userCacheService.isTokenInBlacklist(token)) {
            return false;
        }
        return true;
    }

    /**
     * 获取用户ID
     */
    public Long getUserId(String token) {
        return jwtTokenProvider.getUserIdFromToken(token);
    }

    /**
     * 获取用户名
     */
    public String getUsername(String token) {
        return jwtTokenProvider.getUsernameFromToken(token);
    }

    /**
     * 获取Token ID
     */
    public String getTokenId(String token) {
        return jwtTokenProvider.getTokenId(token);
    }


    /**
     * 将Token加入黑名单
     */
    public void addTokenToBlacklist(String token) {
        long ttl = jwtTokenProvider.getExpirationTime(token) - System.currentTimeMillis();
        if (ttl > 0) {
            userCacheService.addTokenToBlacklist(token, ttl);
        }
    }

    // ==================== RefreshToken管理 ====================

    /**
     * 保存RefreshToken
     */
    public RefreshToken saveRefreshToken(Long userId, String token) {
        LocalDateTime expiryTime = LocalDateTime.now().plusDays(30);
        RefreshToken refreshToken = RefreshToken.builder()
            .userId(userId)
            .token(token)
            .expiryTime(expiryTime)
            .revoked(0)
            .build();
        refreshTokenMapper.insert(refreshToken);
        return refreshToken;
    }

    /**
     * 查找RefreshToken
     */
    public Optional<RefreshToken> findRefreshToken(String token) {
        LambdaQueryWrapper<RefreshToken> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RefreshToken::getToken, token);
        return Optional.ofNullable(refreshTokenMapper.selectOne(wrapper));
    }

    /**
     * 撤销RefreshToken
     */
    public void revokeRefreshToken(RefreshToken refreshToken) {
        refreshToken.revoke();
        refreshTokenMapper.updateById(refreshToken);

        // 加入黑名单
        long ttl = refreshToken.getExpiryTime().atZone(java.time.ZoneId.systemDefault())
            .toInstant().toEpochMilli() - System.currentTimeMillis();
        if (ttl > 0) {
            userCacheService.addRefreshTokenToBlacklist(refreshToken.getToken(), ttl);
        }
    }

    /**
     * 撤销用户所有RefreshToken
     */
    public void revokeAllUserRefreshTokens(Long userId) {
        LambdaQueryWrapper<RefreshToken> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RefreshToken::getUserId, userId).eq(RefreshToken::getRevoked, 0);
        refreshTokenMapper.selectList(wrapper).forEach(this::revokeRefreshToken);
    }

    /**
     * 检查RefreshToken是否在黑名单中
     */
    public boolean isRefreshTokenInBlacklist(String token) {
        return userCacheService.isRefreshTokenInBlacklist(token);
    }
}
