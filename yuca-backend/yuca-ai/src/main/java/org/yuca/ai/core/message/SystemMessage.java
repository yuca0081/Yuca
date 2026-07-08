package org.yuca.ai.core.message;

/**
 * 系统消息。
 * 等价于 langchain4j 的 dev.langchain4j.data.message.SystemMessage。
 */
public record SystemMessage(String text) implements ChatMessage {

    public static SystemMessage from(String text) {
        return new SystemMessage(text);
    }

    @Override
    public Type type() {
        return Type.SYSTEM;
    }
}
