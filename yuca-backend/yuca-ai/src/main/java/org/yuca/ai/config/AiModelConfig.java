package org.yuca.ai.config;

import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.community.model.dashscope.QwenStreamingChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI 模型配置
 * 根据 activeProvider 创建对应的 ChatModel / StreamingChatModel
 */
@Configuration
@EnableConfigurationProperties(AiProperties.class)
public class AiModelConfig {

    @Bean
    public ChatModel chatModel(AiProperties props) {
        return switch (props.getActiveProvider()) {
            case "openai" -> buildOpenAiChatModel(props.getOpenai());
            default -> buildDashscopeChatModel(props.getDashscope());
        };
    }

    @Bean
    public StreamingChatModel streamingChatModel(AiProperties props) {
        return switch (props.getActiveProvider()) {
            case "openai" -> buildOpenAiStreamingChatModel(props.getOpenai());
            default -> buildDashscopeStreamingChatModel(props.getDashscope());
        };
    }

    private ChatModel buildDashscopeChatModel(AiProperties.ProviderConfig config) {
        var builder = QwenChatModel.builder()
                .apiKey(config.getApiKey())
                .modelName(config.getModelName());
        if (config.getBaseUrl() != null) {
            builder.baseUrl(config.getBaseUrl());
        }
        return builder.build();
    }

    private StreamingChatModel buildDashscopeStreamingChatModel(AiProperties.ProviderConfig config) {
        var builder = QwenStreamingChatModel.builder()
                .apiKey(config.getApiKey())
                .modelName(config.getModelName());
        if (config.getBaseUrl() != null) {
            builder.baseUrl(config.getBaseUrl());
        }
        return builder.build();
    }

    private ChatModel buildOpenAiChatModel(AiProperties.ProviderConfig config) {
        var builder = OpenAiChatModel.builder()
                .apiKey(config.getApiKey())
                .modelName(config.getModelName());
        if (config.getBaseUrl() != null) {
            builder.baseUrl(config.getBaseUrl());
        }
        return builder.build();
    }

    private StreamingChatModel buildOpenAiStreamingChatModel(AiProperties.ProviderConfig config) {
        var builder = OpenAiStreamingChatModel.builder()
                .apiKey(config.getApiKey())
                .modelName(config.getModelName());
        if (config.getBaseUrl() != null) {
            builder.baseUrl(config.getBaseUrl());
        }
        return builder.build();
    }
}
