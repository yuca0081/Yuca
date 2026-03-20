package org.yuca.assistant.dto.sse;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * SSE事件基类
 *
 * @author Yuca
 * @since 2025-01-27
 */
@Data
public abstract class SseEvent {

    /**
     * 事件类型
     */
    protected String type;

    /**
     * 开始事件
     */
    @Data
    @EqualsAndHashCode(callSuper = true)
    @NoArgsConstructor
    public static class SseStartEvent extends SseEvent {
        /**
         * 用户消息ID
         */
        private Long messageId;

        public SseStartEvent(Long messageId) {
            this.type = "start";
            this.messageId = messageId;
        }
    }

    /**
     * 思考内容事件（深度思考模式）
     */
    @Data
    @EqualsAndHashCode(callSuper = true)
    @NoArgsConstructor
    public static class SseThinkingEvent extends SseEvent {
        /**
         * 思考内容片段
         */
        private String content;

        public SseThinkingEvent(String content) {
            this.type = "thinking";
            this.content = content;
        }
    }

    /**
     * Token事件
     */
    @Data
    @EqualsAndHashCode(callSuper = true)
    @NoArgsConstructor
    public static class SseTokenEvent extends SseEvent {
        /**
         * 内容片段
         */
        private String content;

        public SseTokenEvent(String content) {
            this.type = "token";
            this.content = content;
        }
    }

    /**
     * 完成事件（包含token使用情况）
     */
    @Data
    @EqualsAndHashCode(callSuper = true)
    @NoArgsConstructor
    public static class SseDoneEvent extends SseEvent {
        /**
         * AI消息ID
         */
        private Long messageId;

        /**
         * 完整消息内容
         */
        private String fullMessage;

        /**
         * 输入token数
         */
        private Integer inputTokens;

        /**
         * 输出token数
         */
        private Integer outputTokens;

        /**
         * 总token数
         */
        private Integer totalTokens;

        public SseDoneEvent(Long messageId, String fullMessage) {
            this.type = "done";
            this.messageId = messageId;
            this.fullMessage = fullMessage;
        }

        public SseDoneEvent(Long messageId, String fullMessage, Integer inputTokens,
                           Integer outputTokens, Integer totalTokens) {
            this.type = "done";
            this.messageId = messageId;
            this.fullMessage = fullMessage;
            this.inputTokens = inputTokens;
            this.outputTokens = outputTokens;
            this.totalTokens = totalTokens;
        }
    }

    /**
     * 错误事件
     */
    @Data
    @EqualsAndHashCode(callSuper = true)
    @NoArgsConstructor
    public static class SseErrorEvent extends SseEvent {
        /**
         * 错误信息
         */
        private String message;

        public SseErrorEvent(String message) {
            this.type = "error";
            this.message = message;
        }
    }
}
