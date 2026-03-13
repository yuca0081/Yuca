package org.yuca.infrastructure.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.yuca.common.exception.BusinessException;
import org.yuca.common.response.ErrorCode;

/**
 * 安全工具类 - 获取当前登录用户信息
 *
 * @author Yuca
 * @since 2025-01-30
 */
public class SecurityUtils {

    private SecurityUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * 获取当前登录用户ID
     * <p>从请求属性中获取，由 JwtAuthenticationFilter 解析并存储</p>
     *
     * @return 当前用户ID
     * @throws BusinessException 如果用户未登录
     */
    public static Long getCurrentUserId() {
        HttpServletRequest request = getCurrentRequest();
        Long userId = (Long) request.getAttribute("userId");

        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return userId;
    }

    /**
     * 获取当前登录用户名
     *
     * @return 当前用户名
     * @throws BusinessException 如果用户未登录
     */
    public static String getCurrentUsername() {
        HttpServletRequest request = getCurrentRequest();
        String username = (String) request.getAttribute("username");

        if (username == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return username;
    }

    /**
     * 获取当前Token ID
     *
     * @return Token ID
     * @throws BusinessException 如果用户未登录
     */
    public static String getCurrentTokenId() {
        HttpServletRequest request = getCurrentRequest();
        String tokenId = (String) request.getAttribute("tokenId");

        if (tokenId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return tokenId;
    }

    /**
     * 获取当前请求对象
     *
     * @return HttpServletRequest
     * @throws IllegalStateException 如果不在请求上下文中
     */
    private static HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes =
            (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        return attributes.getRequest();
    }

    /**
     * 检查当前用户是否已登录
     *
     * @return true-已登录，false-未登录
     */
    public static boolean isAuthenticated() {
        try {
            HttpServletRequest request = getCurrentRequest();
            return request.getAttribute("userId") != null;
        } catch (IllegalStateException e) {
            return false;
        }
    }
}
