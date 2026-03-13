package org.yuca.ai.client.openai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * OpenAI API 消息格式
 *
 * @author Yuca
 * @since 2025-01-27
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpenAIMessage {

    /**
     * 角色：system, user, assistant
     */
    private String role;

    /**
     * 内容
     */
    private String content;
}
