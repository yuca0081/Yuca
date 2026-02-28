package org.yuca.yuca.ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 聊天响应
 *
 * @author Yuca
 * @since 2025-01-27
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {

    /**
     * 回复内容
     */
    private String content;

    /**
     * Token 使用情况
     */
    private Usage usage;

    /**
     * 请求 ID
     */
    private String requestId;

    /**
     * 模型名称
     */
    private String model;
}
