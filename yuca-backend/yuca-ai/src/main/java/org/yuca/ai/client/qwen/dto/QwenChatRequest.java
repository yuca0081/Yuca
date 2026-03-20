package org.yuca.ai.client.qwen.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.yuca.ai.model.ChatRequest;

import java.util.List;
import java.util.Map;

/**
 * 千问聊天请求 - 完全独立
 * <p>
 * 包含千问API的所有字段（OpenAI通用字段 + 千问特有字段）
 * 不继承ChatRequest，完全对齐千问官方文档
 * <p>
 * 字段使用驼峰命名，序列化时自动转换为下划线格式（snake_case）
 *
 * @author Yuca
 * @since 2025-01-27
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class QwenChatRequest {

    // ========== OpenAI通用字段 ==========

    /**
     * 模型名称（必选）
     */
    private String model;

    /**
     * 消息列表（千问格式）
     */
    private List<QwenMessage> messages;

    /**
     * 温度参数（0-2，越大越随机）
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Double temperature;

    /**
     * 核采样概率阈值（0-1，越大越随机）
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Double topP;

    /**
     * 最大 token 数
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer maxTokens;

    /**
     * 内容重复度控制（-2.0 到 2.0，正值降低重复度）
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Double presencePenalty;

    /**
     * 停止词
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<String> stop;

    /**
     * 随机数种子
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer seed;

    /**
     * 是否流式输出
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean stream;

    /**
     * 工具选择策略
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Object toolChoice;

    /**
     * 是否开启并行工具调用
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean parallelToolCalls;

    // ========== 千问特有字段 ==========

    /**
     * 候选Token数量（千问特有）
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer topK;

    /**
     * 是否启用深度思考模式（千问特有）
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean enableThinking;

    /**
     * 思考过程的最大Token数（千问特有）
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer thinkingBudget;

    /**
     * 是否启用联网搜索（千问特有）
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean enableSearch;

    /**
     * 搜索选项（千问特有）
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private SearchOptions searchOptions;

    /**
     * 是否启用代码解释器（千问特有）
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean enableCodeInterpreter;

    /**
     * VL高分辨率图像（千问特有）
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean vlHighResolutionImages;

    /**
     * 流式输出配置（千问特有）
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private StreamOptions streamOptions;

    /**
     * 响应格式（千问特有）
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private ResponseFormat responseFormat;

    /**
     * 工具列表（千问格式）
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<ChatRequest.Tool> tools;

    // ========== 千问特有内部类 ==========

    /**
     * 千问格式的消息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class QwenMessage {
        /**
         * 角色：system, user, assistant, tool
         */
        private String role;

        /**
         * 内容（字符串或数组）
         */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private Object content;

        /**
         * 推理内容（深度思考模式）
         */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private String reasoningContent;

        /**
         * 拒绝内容
         */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private String refusal;

        /**
         * 音频内容（Qwen-Omni模型）
         */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private Audio audio;

        /**
         * 工具调用ID（仅当role为tool时使用）
         */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private String toolCallId;

        /**
         * 名称（可选）
         */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private String name;

        /**
         * 工具调用列表（仅当role为assistant时使用）
         */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private List<ChatRequest.ToolCall> toolCalls;

        /**
         * 函数调用（即将废弃）
         */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private Object functionCall;
    }

    /**
     * 音频内容（Qwen-Omni模型）
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class Audio {
        /**
         * Base64编码的音频数据
         */
        private String data;

        /**
         * 过期时间戳
         */
        private Long expiresAt;
    }


    /**
     * 搜索选项
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class SearchOptions {
        /**
         * 是否强制开启搜索
         */
        private Boolean forcedSearch;

        /**
         * 搜索策略：turbo、max、agent、agent_max
         */
        private String searchStrategy;

        /**
         * 是否开启垂域搜索
         */
        private Boolean enableSearchExtension;
    }

    /**
     * 流式输出配置
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class StreamOptions {
        /**
         * 是否在最后一个数据块包含Token消耗信息
         */
        private Boolean includeUsage;
    }

    /**
     * 响应格式
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class ResponseFormat {
        /**
         * 类型：text、json_object、json_schema
         */
        private String type;

        /**
         * JSON Schema定义（当type为json_schema时）
         */
        private JsonSchema jsonSchema;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
        public static class JsonSchema {
            /**
             * Schema名称
             */
            private String name;

            /**
             * Schema描述
             */
            private String description;

            /**
             * 是否严格遵循Schema
             */
            private Boolean strict;

            /**
             * Schema定义（符合JSON Schema标准）
             */
            private Map<String, Object> schema;
        }
    }
}

