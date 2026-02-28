package org.yuca.yuca.ai.client.openai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * OpenAI 配置类
 *
 * <p>支持 OpenAI 格式的 API（包括官方 OpenAI 和兼容 OpenAI 格式的其他厂商）
 *
 * @author Yuca
 * @since 2025-01-27
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "openai")
public class OpenAIConfig {

    /**
     * API Key
     */
    private String apiKey;

    /**
     * API 基础 URL（可选，默认为 https://api.openai.com/v1）
     * <p>如果使用其他厂商兼容 OpenAI 格式的 API，可以修改此值
     * <p>例如：
     * <ul>
     *   <li>通义千问: https://dashscope.aliyuncs.com/compatible-mode/v1</li>
     *   <li>DeepSeek: https://api.deepseek.com/v1</li>
     *   <li>智谱: https://open.bigmodel.cn/api/paas/v4</li>
     * </ul>
     */
    private String baseUrl = "https://api.openai.com/v1";

    /**
     * 默认模型
     */
    private String model = "gpt-3.5-turbo";

    /**
     * 最大 token 数
     */
    private Integer maxTokens = 2000;

    /**
     * 温度参数（0-1 之间，越大越随机）
     */
    private Float temperature = 0.7f;

    /**
     * 创建 RestTemplate Bean
     *
     * @return RestTemplate 实例
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
