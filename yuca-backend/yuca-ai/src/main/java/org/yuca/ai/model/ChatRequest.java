package org.yuca.ai.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * AI聊天请求 - OpenAI标准格式
 * <p>
 * 严格遵守OpenAI API规范，用于OpenAI、Azure、DeepSeek等兼容厂商
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
public class ChatRequest {

    // ========== 基础参数 ==========

    /**
     * 模型名称（必选）
     */
    private String model;

    /**
     * 消息列表（必选）
     */
    private List<ChatMessage> messages;

    // ========== 通用参数 ==========

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

    // ========== 工具相关 ==========

    /**
     * 工具列表
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<Tool> tools;

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

    // ========== 内部类 ==========

    /**
     * OpenAI格式的消息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class ChatMessage {
        /**
         * 角色：system, user, assistant, tool
         */
        private String role;

        /**
         * 内容（字符串或数组，用于多模态）
         */
        private Object content;

        /**
         * 工具调用ID（仅当role为tool时使用）
         */
        private String toolCallId;

        /**
         * 名称（可选）
         */
        private String name;

        /**
         * 工具调用列表（仅当role为assistant时使用）
         */
        private List<ToolCall> toolCalls;
    }

    /**
     * 工具调用
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class ToolCall {
        /**
         * 工具调用ID
         */
        private String id;

        /**
         * 工具类型
         */
        private String type;

        /**
         * 函数信息
         */
        private Function function;

        /**
         * 索引
         */
        private Integer index;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
        public static class Function {
            /**
             * 函数名称
             */
            private String name;

            /**
             * 函数参数（JSON字符串）
             */
            private String arguments;
        }
    }

    /**
     * 工具定义（OpenAI标准格式）
     * <p>
     * 纯数据容器，用于序列化为JSON
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class Tool {
        /**
         * 工具类型（目前只支持function）
         */
        private String type;

        /**
         * 函数定义
         */
        private Function function;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
        public static class Function {
            /**
             * 函数名称
             */
            private String name;

            /**
             * 函数描述
             */
            private String description;

            /**
             * 参数定义（JSON Schema）
             */
            private Map<String, Object> parameters;
        }
    }
}
