package org.yuca.ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * AI聊天非流式响应基类
 * <p>
 * 用于非流式调用的完整响应，choices中包含完整的message对象
 *
 * @author Yuca
 * @since 2025-01-27
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse extends BaseChatResponse {

    /**
     * 模型生成内容的数组
     */
    private List<Choice> choices;

    /**
     * 包含完整的message对象
     */
    @Data
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Choice {

        /**
         * 完成原因：stop、length、tool_calls
         */
        private String finishReason;

        /**
         * 索引
         */
        private Integer index;

        /**
         * Token 概率信息
         */
        private LogProbs logprobs;

        /**
         * 消息（非流式响应）
         */
        private Message message;

    }

    /**
     * Token 概率信息
     */
    @Data
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LogProbs {
        /**
         * 内容的Token概率信息
         */
        private List<TokenInfo> content;
    }

    /**
     * Token信息
     */
    @Data
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenInfo {
        /**
         * Token文本
         */
        private String token;

        /**
         * UTF-8原始字节列表
         */
        private List<Integer> bytes;

        /**
         * 对数概率
         */
        private Float logprob;

        /**
         * 最可能的若干候选Token
         */
        private List<TopTokenInfo> topLogprobs;
    }

    /**
     * 顶部Token信息
     */
    @Data
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopTokenInfo {
        /**
         * Token文本
         */
        private String token;

        /**
         * UTF-8原始字节列表
         */
        private List<Integer> bytes;

        /**
         * 对数概率
         */
        private Float logprob;
    }






    /**
     * 消息（非流式响应）
     */
    @Data
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Message {

        /**
         * 内容（字符串或数组）
         */
        private String content;

        /**
         * 角色
         */
        private String role;

        /**
         * 推理内容（深度思考模式）
         */
        private String reasoningContent;

        /**
         * 拒绝内容（当前固定为null）
         */
        private String refusal;

        /**
         * 该参数当前固定为null
         */
        private String audio;

        /**
         * 工具调用列表
         */
        private List<ToolCall> toolCalls;

        /**
         * 工具调用
         */
        @Data
        @SuperBuilder
        @NoArgsConstructor
        @AllArgsConstructor
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
            @SuperBuilder
            @NoArgsConstructor
            @AllArgsConstructor
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

    }

    // ========== 充血方法 ==========

    /**
     * 获取第一条消息的内容（快捷方法）
     */
    public String getContent() {
        if (choices == null || choices.isEmpty()) {
            return null;
        }
        Message message = choices.get(0).getMessage();
        return message != null ? message.getContent() : null;
    }

    /**
     * 获取推理内容（深度思考模式）
     */
    public String getReasoningContent() {
        if (choices == null || choices.isEmpty()) {
            return null;
        }
        Message message = choices.get(0).getMessage();
        return message != null ? message.getReasoningContent() : null;
    }

    /**
     * 获取所有工具调用
     */
    public List<Message.ToolCall> getToolCallsList() {
        if (choices == null || choices.isEmpty()) {
            return List.of();
        }
        Message message = choices.get(0).getMessage();
        return message != null && message.getToolCalls() != null
            ? message.getToolCalls()
            : List.of();
    }

    /**
     * 判断是否有工具调用
     */
    public boolean hasToolCalls() {
        return !getToolCallsList().isEmpty();
    }

    /**
     * 判断是否包含推理内容
     */
    public boolean hasReasoningContent() {
        return getReasoningContent() != null;
    }

    /**
     * 获取完成原因
     */
    public String getFinishReason() {
        if (choices == null || choices.isEmpty()) {
            return null;
        }
        return choices.get(0).getFinishReason();
    }

    /**
     * 转换为StreamToken列表（用于缓存场景）
     */
    public List<StreamToken> toStreamTokens() {
        List<StreamToken> tokens = new ArrayList<>();
        String reasoningContent = getReasoningContent();
        String content = getContent();

        if (reasoningContent != null) {
            tokens.add(StreamToken.reasoning(reasoningContent));
        }
        if (content != null) {
            for (char c : content.toCharArray()) {
                tokens.add(StreamToken.content(String.valueOf(c)));
            }
        }
        return tokens;
    }

}
