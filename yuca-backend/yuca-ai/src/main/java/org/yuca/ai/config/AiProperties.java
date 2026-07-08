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
    private EmbeddingConfig embedding = new EmbeddingConfig();
    private RerankConfig rerank = new RerankConfig();

    @Data
    public static class ProviderConfig {
        /** OpenAI 兼容端点 base url */
        private String baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";
        private String modelName;
        private String apiKey;
    }

    @Data
    public static class EmbeddingConfig {
        private String modelName = "text-embedding-v3";
        private int dimension = 1024;
    }

    @Data
    public static class RerankConfig {
        /** 是否启用 Cross-Encoder 重排。关闭后 KnowledgeRetrievalService 走纯 RRF 路径 */
        private boolean enabled = true;
        /** DashScope 原生 rerank 端点的 base url（非 OpenAI 兼容端点） */
        private String baseUrl = "https://dashscope.aliyuncs.com";
        private String modelName = "gte-rerank-v2";
        /** 送入 reranker 的候选池大小。文章《1500 行代码》实验：候选池太大会稀释 reranker 注意力，R@5 反降 */
        private int candidatePoolSize = 20;
    }

    @Data
    public static class MemoryConfig {
        private int maxMessages = 50;
    }
}
