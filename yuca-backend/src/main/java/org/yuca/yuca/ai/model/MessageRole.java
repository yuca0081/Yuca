package org.yuca.yuca.ai.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * AI 消息角色枚举
 *
 * @author Yuca
 * @since 2025-01-27
 */
@Getter
@AllArgsConstructor
public enum MessageRole {

    /**
     * 用户
     */
    USER("user"),

    /**
     * 助手
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

    /**
     * 角色代码
     */
    private final String code;
}
