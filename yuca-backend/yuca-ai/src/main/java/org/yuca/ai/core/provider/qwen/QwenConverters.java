package org.yuca.ai.core.provider.qwen;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.yuca.ai.core.message.AiMessage;
import org.yuca.ai.core.message.ChatMessage;
import org.yuca.ai.core.message.SystemMessage;
import org.yuca.ai.core.message.ToolExecutionResultMessage;
import org.yuca.ai.core.message.UserMessage;
import org.yuca.ai.core.model.ChatRequest;
import org.yuca.ai.core.model.ChatResponse;
import org.yuca.ai.core.model.TokenUsage;
import org.yuca.ai.core.provider.openai.dto.ChatCompletionRequest;
import org.yuca.ai.core.provider.openai.dto.ChatCompletionResponse;
import org.yuca.ai.core.provider.openai.dto.ChatMessageDto;
import org.yuca.ai.core.provider.openai.dto.FunctionCall;
import org.yuca.ai.core.provider.openai.dto.ToolCall;
import org.yuca.ai.core.provider.openai.dto.ToolSpecDto;
import org.yuca.ai.core.provider.openai.dto.Usage;
import org.yuca.ai.core.tool.ToolExecutionRequest;

import java.util.List;

/**
 * core.* ↔ OpenAI 兼容 DTO 之间的转换，以及 Qwen 扩展字段（enable_thinking）的注入。
 * 共享给 QwenChatModel / QwenStreamingChatModel 使用。
 */
final class QwenConverters {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private QwenConverters() {}

    /**
     * 构造请求体（已注入 Qwen 扩展字段）。
     * 返回 JsonNode 而非 DTO，便于各 provider 在标准 OpenAI 字段之外附加自己的扩展字段。
     */
    static JsonNode toRequestBody(ChatRequest request, String modelName, boolean stream) {
        var builder = ChatCompletionRequest.builder()
                .model(modelName)
                .messages(request.messages().stream().map(QwenConverters::toMessageDto).toList())
                .stream(stream);
        if (!request.toolSpecifications().isEmpty()) {
            builder.tools(request.toolSpecifications().stream()
                    .map(spec -> ToolSpecDto.function(spec.name(), spec.description(), spec.parameters()))
                    .toList());
        }
        ChatCompletionRequest base = builder.build();
        ObjectNode node = (ObjectNode) MAPPER.valueToTree(base);
        if (request.parameters() instanceof QwenRequestParameters qwen && qwen.enableThinking() != null) {
            node.put("enable_thinking", qwen.enableThinking());
        }
        return node;
    }

    static ChatMessageDto toMessageDto(ChatMessage msg) {
        return switch (msg) {
            case UserMessage m -> ChatMessageDto.user(m.text());
            case SystemMessage m -> ChatMessageDto.system(m.text());
            case AiMessage m -> m.hasToolExecutionRequests()
                    ? ChatMessageDto.assistant(m.text(),
                            m.toolExecutionRequests().stream().map(QwenConverters::toToolCallDto).toList())
                    : ChatMessageDto.assistant(m.text(), null);
            case ToolExecutionResultMessage m -> ChatMessageDto.tool(m.text());
        };
    }

    private static ToolCall toToolCallDto(org.yuca.ai.core.tool.ToolExecutionRequest req) {
        return new ToolCall(null, req.id(), "function",
                new FunctionCall(req.name(), req.arguments()));
    }

    static AiMessage toAiMessage(ChatMessageDto msg) {
        if (msg.toolCalls() != null && !msg.toolCalls().isEmpty()) {
            var requests = msg.toolCalls().stream()
                    .map(QwenConverters::toToolExecutionRequest)
                    .toList();
            return AiMessage.from(msg.content(), requests);
        }
        return AiMessage.from(msg.content());
    }

    private static ToolExecutionRequest toToolExecutionRequest(ToolCall tc) {
        return ToolExecutionRequest.builder()
                .id(tc.id())
                .name(tc.function() != null ? tc.function().name() : null)
                .arguments(tc.function() != null ? tc.function().arguments() : null)
                .build();
    }

    static ChatResponse toChatResponse(ChatCompletionResponse resp) {
        if (resp == null || resp.choices() == null || resp.choices().isEmpty()) {
            return new ChatResponse(null, null);
        }
        ChatMessageDto msg = resp.choices().get(0).message();
        return new ChatResponse(toAiMessage(msg), toTokenUsage(resp.usage()));
    }

    static TokenUsage toTokenUsage(Usage usage) {
        if (usage == null) return null;
        return TokenUsage.of(usage.promptTokens(), usage.completionTokens(), usage.totalTokens());
    }
}
