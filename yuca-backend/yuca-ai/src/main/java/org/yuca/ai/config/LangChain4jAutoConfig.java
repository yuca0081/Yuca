package org.yuca.ai.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * LangChain4j 自动配置类
 * 负责初始化和管理所有 AI 模型实例
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(LangChain4jProperties.class)
public class LangChain4jAutoConfig {

    /**
     * 创建聊天模型 Bean 映射
     * 支持多个模型提供商
     */
    @Bean
    public Map<String, ChatLanguageModel> chatLanguageModels(LangChain4jProperties properties) {
        Map<String, ChatLanguageModel> models = new HashMap<>();

        for (String provider : properties.getEnabledProviders()) {
            try {
                ChatLanguageModel model = createChatModel(provider, properties);
                if (model != null) {
                    models.put(provider, model);
                    log.info("Initialized chat model for provider: {}", provider);
                }
            } catch (Exception e) {
                log.error("Failed to initialize chat model for provider: {}", provider, e);
            }
        }

        if (models.isEmpty()) {
            log.warn("No chat models were initialized. Please check your configuration.");
        }

        return models;
    }

    /**
     * 创建流式聊天模型 Bean 映射
     */
    @Bean
    public Map<String, StreamingChatLanguageModel> streamingChatLanguageModels(LangChain4jProperties properties) {
        Map<String, StreamingChatLanguageModel> models = new HashMap<>();

        for (String provider : properties.getEnabledProviders()) {
            try {
                StreamingChatLanguageModel model = createStreamingChatModel(provider, properties);
                if (model != null) {
                    models.put(provider, model);
                    log.info("Initialized streaming chat model for provider: {}", provider);
                }
            } catch (Exception e) {
                log.error("Failed to initialize streaming chat model for provider: {}", provider, e);
            }
        }

        if (models.isEmpty()) {
            log.warn("No streaming chat models were initialized. Please check your configuration.");
        }

        return models;
    }

    /**
     * 创建聊天模型
     */
    private ChatLanguageModel createChatModel(String provider, LangChain4jProperties properties) {
        return switch (provider.toLowerCase()) {
            case "openai" -> createOpenAIModel(properties.getOpenAi());
            case "qwen" -> createQwenModel(properties.getQwen());
            default -> {
                log.warn("Unknown provider: {}", provider);
                yield null;
            }
        };
    }

    /**
     * 创建流式聊天模型
     */
    private StreamingChatLanguageModel createStreamingChatModel(String provider, LangChain4jProperties properties) {
        return switch (provider.toLowerCase()) {
            case "openai" -> createOpenAIStreamingModel(properties.getOpenAi());
            case "qwen" -> createQwenStreamingModel(properties.getQwen());
            default -> {
                log.warn("Unknown provider: {}", provider);
                yield null;
            }
        };
    }

    /**
     * 创建 OpenAI 聊天模型
     */
    private ChatLanguageModel createOpenAIModel(LangChain4jProperties.ProviderConfig config) {
        return OpenAiChatModel.builder()
                .apiKey(config.getApiKey())
                .baseUrl(config.getBaseUrl())
                .modelName(config.getModelName())
                .temperature(config.getTemperature())
                .maxTokens(config.getMaxTokens())
                .topP(config.getTopP())
                .stop(config.getStopSequences())
                .timeout(config.getTimeout())
                .build();
    }

    /**
     * 创建 OpenAI 流式聊天模型
     */
    private StreamingChatLanguageModel createOpenAIStreamingModel(LangChain4jProperties.ProviderConfig config) {
        return OpenAiStreamingChatModel.builder()
                .apiKey(config.getApiKey())
                .baseUrl(config.getBaseUrl())
                .modelName(config.getModelName())
                .temperature(config.getTemperature())
                .maxTokens(config.getMaxTokens())
                .topP(config.getTopP())
                .stop(config.getStopSequences())
                .timeout(config.getTimeout())
                .build();
    }

    /**
     * 创建通义千问聊天模型（使用 OpenAI 兼容模式）
     */
    private ChatLanguageModel createQwenModel(LangChain4jProperties.ProviderConfig config) {
        return OpenAiChatModel.builder()
                .apiKey(config.getApiKey())
                .baseUrl(config.getBaseUrl())
                .modelName(config.getModelName())
                .temperature(config.getTemperature())
                .maxTokens(config.getMaxTokens())
                .topP(config.getTopP())
                .stop(config.getStopSequences())
                .timeout(config.getTimeout())
                .build();
    }

    /**
     * 创建通义千问流式聊天模型（使用 OpenAI 兼容模式）
     */
    private StreamingChatLanguageModel createQwenStreamingModel(LangChain4jProperties.ProviderConfig config) {
        return OpenAiStreamingChatModel.builder()
                .apiKey(config.getApiKey())
                .baseUrl(config.getBaseUrl())
                .modelName(config.getModelName())
                .temperature(config.getTemperature())
                .maxTokens(config.getMaxTokens())
                .topP(config.getTopP())
                .stop(config.getStopSequences())
                .timeout(config.getTimeout())
                .build();
    }
}
