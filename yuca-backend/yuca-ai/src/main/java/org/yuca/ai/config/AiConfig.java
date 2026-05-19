package org.yuca.ai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.yuca.ai.embedding.EmbeddingService;

/**
 * AI 模块配置
 */
@Configuration
public class AiConfig {

    @Bean
    public EmbeddingService embeddingService(AiProperties aiProperties) {
        return new EmbeddingService(aiProperties);
    }
}
