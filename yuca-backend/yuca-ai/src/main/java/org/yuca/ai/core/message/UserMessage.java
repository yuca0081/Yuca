package org.yuca.ai.core.message;

/**
 * 用户消息。
 * 等价于 langchain4j 的 dev.langchain4j.data.message.UserMessage。
 */
public record UserMessage(String text) implements ChatMessage {

    public static UserMessage from(String text) {
        return new UserMessage(text);
    }

    @Override
    public Type type() {
        return Type.USER;
    }
}
