package org.yuca.ai.core.message;

import org.yuca.ai.core.tool.ToolExecutionRequest;

import java.util.List;

/**
 * AI 模型生成的消息。
 * 等价于 langchain4j 的 dev.langchain4j.data.message.AiMessage。
 */
public record AiMessage(String text, List<ToolExecutionRequest> toolExecutionRequests) implements ChatMessage {

    private static final List<ToolExecutionRequest> EMPTY = List.of();

    public AiMessage {
        toolExecutionRequests = toolExecutionRequests == null ? EMPTY : List.copyOf(toolExecutionRequests);
    }

    public static AiMessage from(String text) {
        return new AiMessage(text, EMPTY);
    }

    public static AiMessage from(String text, List<ToolExecutionRequest> toolExecutionRequests) {
        return new AiMessage(text, toolExecutionRequests);
    }

    public static AiMessage from(List<ToolExecutionRequest> toolExecutionRequests) {
        return new AiMessage(null, toolExecutionRequests);
    }

    /** 是否包含工具调用请求 */
    public boolean hasToolExecutionRequests() {
        return toolExecutionRequests != null && !toolExecutionRequests.isEmpty();
    }

    @Override
    public Type type() {
        return Type.AI;
    }
}
