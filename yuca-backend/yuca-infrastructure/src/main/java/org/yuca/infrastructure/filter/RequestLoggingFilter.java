package org.yuca.infrastructure.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 请求日志过滤器
 * 以格式化的方式记录每个HTTP请求的详细信息
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger("REQUEST_LOGGER");
    private static final int MAX_CONTENT_LENGTH = 1000; // 最大内容日志长度

    // 需要记录的关键请求头
    private static final List<String> KEY_HEADERS = List.of(
        "Authorization",
        "Content-Type",
        "Accept",
        "User-Agent",
        "Origin",
        "Referer"
    );

    private void logRequest(ContentCachingRequestWrapper request) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String queryString = request.getQueryString();

        // 构建请求日志
        StringBuilder logBuilder = new StringBuilder();
        logBuilder.append("\n");
        logBuilder.append("================  Request Start  ================\n");
        logBuilder.append(String.format("===> %s: %s\n", method, uri + (queryString != null ? "?" + queryString : "")));

        // 添加请求体（如果有）
        byte[] content = request.getContentAsByteArray();
        if (content.length > 0) {
            String requestBody = new String(content, StandardCharsets.UTF_8);
            String truncatedBody = truncateString(requestBody, MAX_CONTENT_LENGTH);
            logBuilder.append(String.format("====Body=====  %s\n", truncatedBody));
            if (requestBody.length() > MAX_CONTENT_LENGTH) {
                logBuilder.append(String.format("... (truncated, total %d bytes)\n", requestBody.length()));
            }
        }

        // 添加关键请求头
        for (String headerName : KEY_HEADERS) {
            String headerValue = request.getHeader(headerName);
            if (headerValue != null) {
                // 对于 Authorization 等敏感信息，只显示前20个字符
                if ("Authorization".equalsIgnoreCase(headerName) && headerValue.length() > 20) {
                    headerValue = headerValue.substring(0, 20) + "...";
                }
                logBuilder.append(String.format("===Headers===  %s: %s\n", headerName, headerValue));
            }
        }

        logBuilder.append("================   Request End   ================\n");

        log.info(logBuilder.toString());
    }

    private void logResponse(ContentCachingRequestWrapper request,
                            ContentCachingResponseWrapper response,
                            long startTime) {
        long duration = System.currentTimeMillis() - startTime;
        String method = request.getMethod();
        String uri = request.getRequestURI();
        int status = response.getStatus();

        // 构建响应日志
        StringBuilder logBuilder = new StringBuilder();
        logBuilder.append("\n");
        logBuilder.append("===============  Response Start  ================\n");

        // 添加响应体（如果有）
        byte[] content = response.getContentAsByteArray();
        if (content.length > 0) {
            String responseBody = new String(content, StandardCharsets.UTF_8);
            String truncatedBody = truncateString(responseBody, MAX_CONTENT_LENGTH);
            logBuilder.append(String.format("===Result===  %s\n", truncatedBody));
            if (responseBody.length() > MAX_CONTENT_LENGTH) {
                logBuilder.append(String.format("... (truncated, total %d bytes)\n", responseBody.length()));
            }
        }

        // 添加用户信息（如果有）
        Object userIdObj = request.getAttribute("userId");
        String username = (String) request.getAttribute("username");
        if (userIdObj != null || username != null) {
            String userId = userIdObj != null ? String.valueOf(userIdObj) : null;
            logBuilder.append(String.format("===User===  id=%s, username=%s\n",
                userId != null ? userId : "N/A",
                username != null ? username : "N/A"));
        }

        logBuilder.append(String.format("<=== %s: %s (%d ms)\n", method, uri, duration));
        logBuilder.append("===============   Response End   ================\n");

        // 根据状态码选择日志级别
        if (status >= 500) {
            log.error(logBuilder.toString());
        } else if (status >= 400) {
            log.warn(logBuilder.toString());
        } else {
            log.info(logBuilder.toString());
        }
    }

    /**
     * 截断字符串到指定长度
     */
    private String truncateString(String str, int maxLength) {
        if (str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // 过滤掉不需要记录的路径
        return path.contains("/swagger-ui") ||
               path.contains("/v3/api-docs") ||
               path.contains("/swagger-resources") ||
               path.contains("/webjars") ||
               path.equals("/favicon.ico") ||
               path.equals("/csrf");
    }

    /**
     * 对于SSE流式响应，不要包装response，避免缓冲
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long startTime = System.currentTimeMillis();

        String path = request.getRequestURI();

        // SSE流式响应不使用ContentCachingResponseWrapper，避免缓冲
        if (path.contains("/assistant/chat")) {
            // 只包装request用于日志记录
            ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);

            try {
                filterChain.doFilter(wrappedRequest, response);
                logRequest(wrappedRequest);
            } catch (Exception e) {
                throw e;
            }
            return;
        }

        // 非SSE请求，正常使用wrapper
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        try {
            // 继续过滤器链
            filterChain.doFilter(wrappedRequest, wrappedResponse);

            // 记录请求日志（在处理完成后，此时请求体已被缓存）
            logRequest(wrappedRequest);

            // 记录响应日志（在处理完成后，此时响应体已被缓存）
            logResponse(wrappedRequest, wrappedResponse, startTime);
        } finally {
            // 重要：必须复制缓存的内容到原始响应
            wrappedResponse.copyBodyToResponse();
        }
    }
}
