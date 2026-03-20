package org.yuca.assistant.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * AI助手配置类
 *
 * @author Yuca
 * @since 2025-01-27
 */
@Configuration
@ConditionalOnProperty(name = "qwen.api-key")
public class AssistantConfig {

    /**
     * 配置千问专用 RestTemplate
     * <p>
     * 读取超时设置为 5 分钟，适应流式响应
     *
     * @param builder Spring Boot 的 RestTemplate 构建器
     * @return 配置好的 RestTemplate
     */
    @Bean
    public RestTemplate qwenRestTemplate(RestTemplateBuilder builder) {
        return builder
            .setConnectTimeout(Duration.ofSeconds(10))
            .setReadTimeout(Duration.ofMinutes(5)) // 流式响应可能较慢
            .build();
    }
}
