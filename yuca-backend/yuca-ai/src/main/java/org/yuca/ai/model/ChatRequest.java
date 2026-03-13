package org.yuca.ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.yuca.ai.tool.IAITool;

import java.util.List;

/**
 * AI 聊天请求
 *
 * @author Yuca
 * @since 2025-01-27
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {

    /**
     * 消息列表
     */
    private List<AIMessage> messages;

    /**
     * 模型名称（可选，默认使用配置的模型）
     */
    private String model;

    /**
     * 最大 token 数（可选）
     */
    private Integer maxTokens;

    /**
     * 温度参数（可选，0-1 之间，越大越随机）
     */
    private Double temperature;

    /**
     * 是否启用搜索（可选）
     */
    private Boolean enableSearch;

    /**
     * 工具列表（可选）
     */
    private List<IAITool> tools;
}
