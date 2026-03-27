package org.yuca.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * DashScope配置属性
 */
@Data
@ConfigurationProperties(prefix = "langchain4j.dashscope.chat-model")
public class DashscopeProperties {

    /**
     * API基础URL
     */
    private String baseUrl;

    /**
     * 模型名称
     */
    private String modelName;

    /**
     * API密钥
     */
    private String apiKey;
}
