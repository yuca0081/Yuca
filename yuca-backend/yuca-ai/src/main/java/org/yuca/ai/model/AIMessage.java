package org.yuca.ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 消息模型
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
     * 内容
     */
    private String content;

    /**
     * 创建用户消息
     *
     * @param content 消息内容
     * @return 用户消息
     */
    public static AIMessage user(String content) {
        return AIMessage.builder()
            .role(MessageRole.USER)
            .content(content)
            .build();
    }

    /**
     * 创建助手消息
     *
     * @param content 消息内容
     * @return 助手消息
     */
    public static AIMessage assistant(String content) {
        return AIMessage.builder()
            .role(MessageRole.ASSISTANT)
            .content(content)
            .build();
    }

    /**
     * 创建系统消息
     *
     * @param content 消息内容
     * @return 系统消息
     */
    public static AIMessage system(String content) {
        return AIMessage.builder()
            .role(MessageRole.SYSTEM)
            .content(content)
            .build();
    }
}
