package org.yuca.yuca.ai.client.qwen;

import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.embeddings.TextEmbedding;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 通义千问配置类
 * @author Yuca
 * @since 2025-01-27
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "qwen")
public class QwenConfig {

    /**
     * API Key
     */
    private String apiKey;

    /**
     * 默认模型
     */
    private String model = "qwen3.5-plus";
    /**
     * 最大 token 数
     */
    private Integer maxTokens = 2000;

    /**
     * 温度参数（0-1 之间，越大越随机）
     */
    private Float temperature = 0.7f;

    /**
     * 创建 DashScope MultiModalConversation 实例（多模态）
     *
     * @return MultiModalConversation 实例
     */
    @Bean
    public MultiModalConversation multiModalConversation() {
        return new MultiModalConversation();
    }

    /**
     * 创建 DashScope TextEmbedding 实例
     *
     * @return TextEmbedding 实例
     */
    @Bean
    public TextEmbedding textEmbedding() {
        return new TextEmbedding();
    }
}
