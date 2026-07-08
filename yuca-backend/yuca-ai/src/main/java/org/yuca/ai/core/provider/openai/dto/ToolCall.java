package org.yuca.ai.core.provider.openai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * OpenAI 兼容格式的一次工具调用。
 * <p>
 * 流式响应里每个 chunk 的 tool_calls 包含 index 字段（按 index 累积 arguments），
 * 同步响应里 index 字段为 null。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ToolCall(
        Integer index,
        String id,
        String type,
        FunctionCall function
) {}
