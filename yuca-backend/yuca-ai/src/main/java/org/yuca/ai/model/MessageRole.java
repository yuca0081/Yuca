package org.yuca.ai.model;

import lombok.Getter;

/**
 * AI消息角色枚举
 *
 * @author Yuca
 * @since 2025-01-27
 */
@Getter
public enum MessageRole {
    /**
     * 用户
     */
    USER("user"),

    /**
     * AI助手
     */
    ASSISTANT("assistant"),

    /**
     * 系统
     */
    SYSTEM("system"),

    /**
     * 工具
     */
    TOOL("tool");

    private final String role;

    MessageRole(String role) {
        this.role = role;
    }

}
