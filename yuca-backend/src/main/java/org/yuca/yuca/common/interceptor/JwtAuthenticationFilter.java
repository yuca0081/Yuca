package org.yuca.yuca.common.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.yuca.yuca.common.annotation.SkipAuth;
import org.yuca.yuca.common.exception.BusinessException;
import org.yuca.yuca.common.response.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.yuca.yuca.infrastructure.security.JwtTokenProvider;
import org.yuca.yuca.user.cache.UserCacheService;

/**
 * JWT认证过滤器
 */
@Component
public class JwtAuthenticationFilter implements HandlerInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserCacheService userCacheService;

    @Value("${jwt.header:Authorization}")
    private String tokenHeader;

    @Value("${jwt.prefix:Bearer }")
    private String tokenPrefix;

    public JwtAuthenticationFilter(
        JwtTokenProvider jwtTokenProvider,
        UserCacheService tokenCache
    ) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userCacheService = tokenCache;
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                            @NonNull HttpServletResponse response,
                            @NonNull Object handler) throws Exception {
        // 跳过Swagger相关路径
        String requestURI = request.getRequestURI();
        if (requestURI.contains("/swagger-ui") ||
            requestURI.contains("/v3/api-docs") ||
            requestURI.contains("/swagger-resources") ||
            requestURI.contains("/webjars") ||
            requestURI.equals("/favicon.ico") ||
            requestURI.equals("/csrf")) {
            return true;
        }

        // 检查是否为HandlerMethod（可能是静态资源等）
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;

        // 检查是否有SkipAuth注解
        SkipAuth skipAuth = handlerMethod.getMethodAnnotation(SkipAuth.class);
        if (skipAuth == null) {
            skipAuth = handlerMethod.getBeanType().getAnnotation(SkipAuth.class);
        }

        // 如果有SkipAuth注解，跳过认证
        if (skipAuth != null) {
            return true;
        }

        // 提取Token
        String token = extractToken(request);
        if (token == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        // 验证Token（签名和过期时间）
        if (!jwtTokenProvider.validateToken(token)) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }

        // 检查Token是否在黑名单中（已登出/被撤销）
        if (userCacheService.isTokenInBlacklist(token)) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }

        // 获取用户信息
        Long userId = jwtTokenProvider.getUserIdFromToken(token);
        String username = jwtTokenProvider.getUsernameFromToken(token);
        String tokenId = jwtTokenProvider.getTokenId(token);

        // 将用户信息存入请求属性
        request.setAttribute("userId", userId);
        request.setAttribute("username", username);
        request.setAttribute("tokenId", tokenId);

        return true;
    }

    /**
     * 从请求头中提取Token
     */
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(tokenHeader);
        if (bearerToken != null && bearerToken.startsWith(tokenPrefix)) {
            return bearerToken.substring(tokenPrefix.length());
        }
        return null;
    }
}
