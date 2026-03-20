package org.yuca.ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI消息（通用格式）
 *
 * @author Yuca
 * @since 2025-01-27
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIMessage {

    /**
     * 角色
     */
    private MessageRole role;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 工具调用ID（可选，用于工具响应）
     */
    private String toolCallId;

    /**
     * 创建用户消息
     */
    public static AIMessage user(String content) {
        return AIMessage.builder()
            .role(MessageRole.USER)
            .content(content)
            .build();
    }

    /**
     * 创建助手消息
     */
    public static AIMessage assistant(String content) {
        return AIMessage.builder()
            .role(MessageRole.ASSISTANT)
            .content(content)
            .build();
    }

    /**
     * 创建系统消息
     */
    public static AIMessage system(String content) {
        return AIMessage.builder()
            .role(MessageRole.SYSTEM)
            .content(content)
            .build();
    }

    /**
     * 创建工具响应消息
     */
    public static AIMessage toolResponse(String toolCallId, String content) {
        return AIMessage.builder()
            .role(MessageRole.TOOL)
            .toolCallId(toolCallId)
            .content(content)
            .build();
    }
}
