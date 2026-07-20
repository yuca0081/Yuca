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
    private IntentClassifierConfig intentClassifier = new IntentClassifierConfig();
    private SummaryConfig summary = new SummaryConfig();

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
        private int candidatePoolSize = 20;
    }

    @Data
    public static class IntentClassifierConfig {
        /**
         * 是否启用意图识别。关闭后 AgentFactory 不会注册 IntentRecognitionEnhancer，
         * RagEnhancer 见 context.intent == null 走原路径（每次都执行 RAG）。
         */
        private boolean enabled = true;
        /**
         * 小模型名（DashScope）。复用 {@link ProviderConfig#getBaseUrl()} 和
         * {@link ProviderConfig#getApiKey()} 的 dashscope 凭证，不独立配置。
         * 推荐 qwen-turbo——便宜快，对短文本意图分类准确率足够。
         */
        private String modelName = "qwen-turbo";
    }

    @Data
    public static class SummaryConfig {
        /**
         * 是否启用历史摘要压缩。关闭后 HistoryEnhancer 走原固定窗口截断路径（加载全量 + trim 50）。
         */
        private boolean enabled = true;
        /**
         * 触发阈值：active 消息数（已排除被早期 SUMMARY 取代的部分）超过此值才触发新一轮摘要。
         * 选 20 是经验值——低于此触发过于频繁，高于此上下文 token 压力大。
         */
        private int threshold = 20;
        /**
         * 触发后保留最近 N 条 raw 消息不参与摘要，保证近期决策原文留存供 LLM 精细参考。
         * 其余 (active.size - keepRecent) 条并入新 SUMMARY。
         */
        private int keepRecent = 10;
        /**
         * 小模型名（DashScope）。复用 dashscope.base-url + dashscope.api-key。
         * 摘要任务对模型推理能力要求不高，qwen-turbo 性价比合适。
         */
        private String modelName = "qwen-turbo";
    }

    @Data
    public static class MemoryConfig {
        private int maxMessages = 50;
    }
}
