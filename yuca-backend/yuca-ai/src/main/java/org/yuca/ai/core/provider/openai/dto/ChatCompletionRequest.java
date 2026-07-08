package org.yuca.ai.core.provider.openai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * OpenAI 兼容的 chat completions 请求体（标准字段）。
 * <p>
 * provider 专属扩展字段（如 Qwen 的 {@code enable_thinking}）不在此处声明，
 * 由各 provider 的 Converters 在序列化后通过 ObjectNode 注入。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ChatCompletionRequest(
        String model,
        List<ChatMessageDto> messages,
        List<ToolSpecDto> tools,
        @JsonProperty("tool_choice") String toolChoice,
        Boolean stream,
        Double temperature,
        @JsonProperty("max_tokens") Integer maxTokens
) {
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String model;
        private List<ChatMessageDto> messages;
        private List<ToolSpecDto> tools;
        private String toolChoice;
        private Boolean stream;
        private Double temperature;
        private Integer maxTokens;

        public Builder model(String model) { this.model = model; return this; }
        public Builder messages(List<ChatMessageDto> messages) { this.messages = messages; return this; }
        public Builder tools(List<ToolSpecDto> tools) { this.tools = tools; return this; }
        public Builder toolChoice(String toolChoice) { this.toolChoice = toolChoice; return this; }
        public Builder stream(Boolean stream) { this.stream = stream; return this; }
        public Builder temperature(Double temperature) { this.temperature = temperature; return this; }
        public Builder maxTokens(Integer maxTokens) { this.maxTokens = maxTokens; return this; }

        public ChatCompletionRequest build() {
            return new ChatCompletionRequest(model, messages, tools, toolChoice, stream,
                    temperature, maxTokens);
        }
    }
}
