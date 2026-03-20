package org.yuca.ai.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * AI聊天流式响应基类
 * <p>
 * 用于流式调用的增量响应，choices中包含增量delta对象
 *
 * @author Yuca
 * @since 2025-01-27
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ChatStreamResponse extends BaseChatResponse {

    /**
     * 选择列表（模型生成内容的数组）
     * <p>
     * 流式：包含增量delta对象
     */
    private List<Choice> choices;

    /**
     * 选择项（流式响应）
     * <p>
     * 包含增量delta对象
     */
    @Data
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Choice {
        /**
         * 索引
         */
        private Integer index;

        /**
         * Delta（流式响应）
         */
        private Delta delta;

        /**
         * 完成原因：stop、length、tool_calls（流式响应最后一个chunk中才有值）
         */
        private String finishReason;

        /**
         * Token 概率信息
         */
        private LogProbs logprobs;
    }

    /**
     * Delta（流式响应）
     * <p>
     * 包含增量消息内容
     */
    @Data
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Delta {
        /**
         * 角色（仅在第一个chunk中有值）
         */
        private String role;

        /**
         * 增量内容
         */
        private String content;

        /**
         * 增量推理内容（深度思考模式）
         */
        private String reasoningContent;

        /**
         * 拒绝内容（当前固定为null）
         */
        private String refusal;

        /**
         * 音频内容（Qwen-Omni模型）
         */
        private String audio;

        /**
         * 增量工具调用列表
         */
        private List<DeltaToolCall> toolCalls;
    }

    /**
     * Delta中的工具调用（流式响应）
     */
    @Data
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeltaToolCall {
        /**
         * 工具调用索引
         */
        private Integer index;

        /**
         * 工具调用ID（仅在第一个chunk中有值）
         */
        private String id;

        /**
         * 工具类型
         */
        private String type;

        /**
         * 函数信息
         */
        private DeltaFunction function;

        @Data
        @SuperBuilder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class DeltaFunction {
            /**
             * 函数名称（仅在第一个chunk中有值）
             */
            private String name;

            /**
             * 增量函数参数（所有chunk的arguments拼接后为完整的入参）
             */
            private String arguments;
        }
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
        private List<ChatResponse.TokenInfo> content;
    }

    // ========== 充血方法 ==========

    /**
     * 获取第一个delta的内容
     */
    public String getDeltaContent() {
        if (choices == null || choices.isEmpty()) {
            return null;
        }
        Delta delta = choices.get(0).getDelta();
        return delta != null ? delta.getContent() : null;
    }

    /**
     * 获取第一个delta的推理内容
     */
    public String getDeltaReasoningContent() {
        if (choices == null || choices.isEmpty()) {
            return null;
        }
        Delta delta = choices.get(0).getDelta();
        return delta != null ? delta.getReasoningContent() : null;
    }

    /**
     * 转换为StreamToken
     */
    public StreamToken toStreamToken() {
        // 优先级1：推理内容（深度思考模式）
        String reasoningContent = getDeltaReasoningContent();
        if (reasoningContent != null) {
            return StreamToken.reasoning(reasoningContent);
        }

        // 优先级2：音频内容（Qwen-Omni音频模型）
        String audio = getDeltaAudio();
        if (audio != null) {
            return StreamToken.content(audio);
        }

        // 优先级3：普通文本内容
        String content = getDeltaContent();
        if (content != null) {
            return StreamToken.content(content);
        }

        return null;
    }

    /**
     * 获取第一个delta的音频内容
     */
    public String getDeltaAudio() {
        if (choices == null || choices.isEmpty()) {
            return null;
        }
        Delta delta = choices.get(0).getDelta();
        return delta != null ? delta.getAudio() : null;
    }

    /**
     * 判断是否有完成原因
     */
    public boolean hasFinishReason() {
        if (choices == null || choices.isEmpty()) {
            return false;
        }
        return choices.get(0).getFinishReason() != null;
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
     * 判断是否有增量内容
     */
    public boolean hasContent() {
        return getDeltaContent() != null;
    }

    /**
     * 判断是否有增量推理内容
     */
    public boolean hasReasoningContent() {
        return getDeltaReasoningContent() != null;
    }

}
