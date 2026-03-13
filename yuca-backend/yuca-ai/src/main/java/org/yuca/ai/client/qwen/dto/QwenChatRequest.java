package org.yuca.ai.client.qwen.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 千问 API 聊天请求格式（兼容 OpenAI）
 *
 * @author Yuca
 * @since 2025-01-27
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QwenChatRequest {

    /**
     * 模型名称
     */
    private String model;

    /**
     * 消息列表
     */
    private List<QwenMessage> messages;

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

    /**
     * 是否启用搜索
     */
    private Boolean enable_search;
}
