package org.yuca.ai.client.qwen;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 通义千问配置类
 *
 * @author Yuca
 * @since 2025-01-27
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "qwen")
public class QwenConfig {

    /**
     * API Key
     */
    private String apiKey;

    /**
     * API 基础地址（兼容OpenAI模式）
     */
    private String baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";

    /**
     * 默认模型
     */
    private String model = "qwen3.5-flash";

    /**
     * 最大 token 数
     */
    private Integer maxTokens = 2000;

    /**
     * 温度参数（0-1 之间，越大越随机）
     */
    private Float temperature = 0.7f;

    /**
     * Embedding 默认模型
     */
    private String embeddingModel = "text-embedding-v3";

    /**
     * Embedding 默认向量维度
     */
    private Integer embeddingDimensions = 1536;
}
