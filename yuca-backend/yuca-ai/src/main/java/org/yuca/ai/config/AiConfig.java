package org.yuca.ai.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.yuca.ai.core.provider.qwen.QwenRerankModel;
import org.yuca.ai.embedding.EmbeddingService;
import org.yuca.ai.retrieval.DefaultRerankService;
import org.yuca.ai.retrieval.RerankService;

/**
 * AI 模块配置
 */
@Configuration
public class AiConfig {

    @Bean
    public EmbeddingService embeddingService(AiProperties aiProperties) {
        return new EmbeddingService(aiProperties);
    }

    /**
     * Cross-Encoder 重排服务。enabled=false 时不创建 bean，
     * KnowledgeRetrievalService 通过 ObjectProvider 探测到 null 后走纯 RRF 路径。
     */
    @Bean
    @ConditionalOnProperty(prefix = "yuca.ai.rerank", name = "enabled", havingValue = "true", matchIfMissing = true)
    public RerankService rerankService(AiProperties aiProperties) {
        AiProperties.RerankConfig cfg = aiProperties.getRerank();
        AiProperties.ProviderConfig dashscope = aiProperties.getDashscope();
        QwenRerankModel model = new QwenRerankModel(
                cfg.getBaseUrl(), dashscope.getApiKey(), cfg.getModelName());
        return new DefaultRerankService(model);
    }
}

