package org.yuca.yuca.ai.client.openai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * OpenAI API 聊天请求格式
 *
 * @author Yuca
 * @since 2025-01-27
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpenAIChatRequest {

    /**
     * 模型名称
     */
    private String model;

    /**
     * 消息列表
     */
    private List<OpenAIMessage> messages;

    /**
     * 温度参数（0-2，越大越随机）
     */
    private Double temperature;

    /**
     * 最大 token 数
     */
    private Integer max_tokens;

    /**
     * 是否流式输出
     */
    private Boolean stream;
}
