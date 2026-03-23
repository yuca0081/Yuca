package org.yuca.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * LangChain4j 配置属性类
 * 从 application.yml 读取配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "yuca.ai")
public class LangChain4jProperties {

    /**
     * 启用的模型提供商列表
     */
    private List<String> enabledProviders = new ArrayList<>();

    /**
     * 默认模型提供商
     */
    private String defaultProvider = "qwen";

    /**
     * 工具扫描包路径
     */
    private String toolsScanPackage = "org.yuca.ai.tool";

    /**
     * OpenAI 配置
     */
    private ProviderConfig openAi = new ProviderConfig();

    /**
     * 通义千问配置
     */
    private ProviderConfig qwen = new ProviderConfig();

    /**
     * 记忆配置
     */
    private MemoryConfig memory = new MemoryConfig();

    /**
     * 模型提供商配置
     */
    @Data
    public static class ProviderConfig {
        /**
         * API 密钥
         */
        private String apiKey;

        /**
         * API 基础 URL
         */
        private String baseUrl;

        /**
         * 模型名称
         */
        private String modelName;

        /**
         * 温度参数 (0.0 - 2.0)
         */
        private Double temperature = 0.7;

        /**
         * 最大 token 数
         */
        private Integer maxTokens = 2000;

        /**
         * 超时时间
         */
        private Duration timeout = Duration.ofSeconds(60);

        /**
         * Top P 采样参数
         */
        private Double topP = 1.0;

        /**
         * 停止序列
         */
        private List<String> stopSequences;
    }

    /**
     * 记忆配置
     */
    @Data
    public static class MemoryConfig {
        /**
         * 最大消息数量
         */
        private Integer maxMessages = 50;

        /**
         * 记忆保留时长
         */
        private Duration retainDuration = Duration.ofHours(24);
    }
}
