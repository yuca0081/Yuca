package org.yuca.ai.core.provider.qwen;

import org.yuca.ai.core.model.ChatRequestParameters;

/**
 * Qwen / DashScope 专属请求参数。
 * 通过 enableThinking 开启思考模式（Qwen3 系列支持）。
 * <p>
 * enableThinking 不会出现在标准 OpenAI DTO 中，而是在 {@code QwenConverters} 序列化请求时
 * 通过 ObjectNode 注入为 {@code enable_thinking} 扩展字段。
 */
public record QwenRequestParameters(Boolean enableThinking) implements ChatRequestParameters {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Boolean enableThinking;

        public Builder enableThinking(boolean enableThinking) {
            this.enableThinking = enableThinking;
            return this;
        }

        public QwenRequestParameters build() {
            return new QwenRequestParameters(enableThinking);
        }
    }
}
