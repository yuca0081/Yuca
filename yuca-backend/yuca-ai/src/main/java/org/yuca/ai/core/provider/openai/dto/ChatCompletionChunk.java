package org.yuca.ai.core.provider.openai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * OpenAI 兼容的 chat completions 流式 chunk（每行 data: 后的 JSON）。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ChatCompletionChunk(
        String id,
        List<ChunkChoice> choices,
        Usage usage
) {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ChunkChoice(
            Integer index,
            ChatMessageDto delta,
            @JsonProperty("finish_reason") String finishReason
    ) {}
}
