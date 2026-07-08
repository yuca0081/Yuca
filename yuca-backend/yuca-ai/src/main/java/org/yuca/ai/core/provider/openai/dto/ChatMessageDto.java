package org.yuca.ai.core.provider.openai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * OpenAI 兼容格式的一条消息（同时用于请求和响应）。
 * <p>
 * {@code reasoning_content} 是 reasoning model（DeepSeek-reasoner / Qwen3 思考模式）的扩展字段，
 * 非标准 OpenAI 协议字段，但多家 provider 复用此名字，因此保留在公共层。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ChatMessageDto(
        String role,
        String content,
        @JsonProperty("tool_calls") List<ToolCall> toolCalls,
        @JsonProperty("reasoning_content") String reasoningContent
) {
    public static ChatMessageDto system(String content) {
        return new ChatMessageDto("system", content, null, null);
    }

    public static ChatMessageDto user(String content) {
        return new ChatMessageDto("user", content, null, null);
    }

    public static ChatMessageDto assistant(String content, List<ToolCall> toolCalls) {
        return new ChatMessageDto("assistant", content, toolCalls, null);
    }

    public static ChatMessageDto tool(String content) {
        return new ChatMessageDto("tool", content, null, null);
    }
}
