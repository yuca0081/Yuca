package org.yuca.ai.core.message;

import org.yuca.ai.core.tool.ToolExecutionRequest;

/**
 * 工具执行结果消息，作为对 {@link ToolExecutionRequest} 的回复。
 * 等价于 langchain4j 的 dev.langchain4j.data.message.ToolExecutionResultMessage。
 */
public record ToolExecutionResultMessage(String id, String toolName, String text) implements ChatMessage {

    public static ToolExecutionResultMessage from(ToolExecutionRequest request, String text) {
        return new ToolExecutionResultMessage(request.id(), request.name(), text);
    }

    public static ToolExecutionResultMessage from(String id, String toolName, String text) {
        return new ToolExecutionResultMessage(id, toolName, text);
    }

    @Override
    public Type type() {
        return Type.TOOL_RESULT;
    }
}
