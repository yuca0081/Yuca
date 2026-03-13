package org.yuca.ai.client.qwen.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 千问 API 消息格式（兼容 OpenAI）
 *
 * @author Yuca
 * @since 2025-01-27
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QwenMessage {

    /**
     * 角色：system, user, assistant
     */
    private String role;

    /**
     * 内容
     */
    private String content;
}
