package org.yuca.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AI 模块统一配置
 */
@Data
@ConfigurationProperties(prefix = "yuca.ai")
public class AiProperties {

    private ProviderConfig dashscope = new ProviderConfig();
    private ProviderConfig openai = new ProviderConfig();

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
