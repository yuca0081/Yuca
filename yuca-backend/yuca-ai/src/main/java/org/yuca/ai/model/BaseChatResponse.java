package org.yuca.ai.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * AI聊天响应基类
 * <p>
 * 提取ChatResponse和ChatStreamResponse的共同字段
 *
 * @author Yuca
 * @since 2025-01-27
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class BaseChatResponse {

    /**
     * 响应ID（本次调用的唯一标识符）
     */
    private String id;

    /**
     * 对象类型（chat.completion 或 chat.completion.chunk）
     */
    private String object;

    /**
     * 创建时间戳（秒）
     */
    private Long created;

    /**
     * 模型名称
     */
    private String model;

    /**
     * 服务层级（当前固定为null）
     */
    private String serviceTier;

    /**
     * 系统指纹（当前固定为null）
     */
    private String systemFingerprint;

    /**
     * Token使用统计
     */
    private Usage usage;

    /**
     * Token使用统计
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class Usage {

        /**
         * 输出token数
         */
        private Integer completionTokens;

        /**
         * 输入token数
         */
        private Integer promptTokens;

        /**
         * 总token数
         */
        private Integer totalTokens;

        /**
         * 输出token详细信息
         */
        private CompletionTokensDetails completionTokensDetails;


        /**
         * 输入token详细信息
         */
        private PromptTokensDetails promptTokensDetails;

        // ========== 充血方法 ==========

        /**
         * 获取输入token数（别名方法，符合OpenAI官方术语）
         */
        public Integer getInputTokens() {
            return promptTokens;
        }

        /**
         * 获取输出token数（别名方法，符合OpenAI官方术语）
         */
        public Integer getOutputTokens() {
            return completionTokens;
        }

        /**
         * 输出token详细信息
         */
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
        public static class CompletionTokensDetails {
            /**
             * 音频token数（Qwen-Omni）该参数当前固定为null
             */
            private Integer audioTokens;

            /**
             * 思考token数
             */
            private Integer reasoningTokens;

            /**
             * 文本token数（Qwen-VL）
             */
            private Integer textTokens;
        }

        /**
         * 输入token详细信息
         */
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
        public static class PromptTokensDetails {
            /**
             * 音频token数
             */
            private Integer audioTokens;

            /**
             * 缓存token数
             */
            private Integer cachedTokens;

            /**
             * 文本token数（Qwen-VL）
             */
            private Integer textTokens;

            /**
             * 图片token数（Qwen-VL）
             */
            private Integer imageTokens;

            /**
             * 视频token数（Qwen-VL）
             */
            private Integer videoTokens;
        }
    }

}
