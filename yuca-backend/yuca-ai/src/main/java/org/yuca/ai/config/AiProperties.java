package org.yuca.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AI 模块统一配置
 */
@Data
@ConfigurationProperties(prefix = "yuca.ai")
public class AiProperties {

    /** 当前激活的模型提供者 (dashscope / openai) */
    private String activeProvider = "dashscope";

    private ProviderConfig dashscope = new ProviderConfig();
    private ProviderConfig openai = new ProviderConfig();
    private MemoryConfig memory = new MemoryConfig();

    @Data
    public static class ProviderConfig {
        private String baseUrl;
        private String modelName;
        private String apiKey;
    }

    @Data
    public static class MemoryConfig {
        private int maxMessages = 50;
    }
}
