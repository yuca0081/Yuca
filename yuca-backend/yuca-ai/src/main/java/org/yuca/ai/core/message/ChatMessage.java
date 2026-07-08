package org.yuca.ai.core.message;

/**
 * 聊天消息的统一抽象。
 * sealed 接口，所有消息类型在 permits 中穷举，配合 pattern switch 使用。
 * 等价于 langchain4j 的 dev.langchain4j.data.message.ChatMessage。
 */
public sealed interface ChatMessage
        permits UserMessage, SystemMessage, AiMessage, ToolExecutionResultMessage {

    /** 消息类型枚举 */
    enum Type { USER, SYSTEM, AI, TOOL_RESULT }

    /** 返回消息类型 */
    Type type();

    /** 返回消息的纯文本内容（AiMessage 无文本时返回空串） */
    default String singleText() {
        return switch (this) {
            case UserMessage m -> m.text();
            case SystemMessage m -> m.text();
            case AiMessage m -> m.text() != null ? m.text() : "";
            case ToolExecutionResultMessage m -> m.text();
        };
    }
}
