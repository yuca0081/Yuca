package org.yuca.ai.core.model;

import org.yuca.ai.core.message.ChatMessage;
import org.yuca.ai.core.tool.ToolSpecification;

import java.util.ArrayList;
import java.util.List;

/**
 * 聊天请求。
 * 等价于 langchain4j 的 dev.langchain4j.model.chat.request.ChatRequest。
 */
public record ChatRequest(
        List<ChatMessage> messages,
        ChatRequestParameters parameters,
        List<ToolSpecification> toolSpecifications
) {

    public ChatRequest {
        messages = messages == null ? List.of() : List.copyOf(messages);
        toolSpecifications = toolSpecifications == null ? List.of() : List.copyOf(toolSpecifications);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private List<ChatMessage> messages = new ArrayList<>();
        private ChatRequestParameters parameters;
        private List<ToolSpecification> toolSpecifications = new ArrayList<>();

        public Builder messages(List<ChatMessage> messages) {
            this.messages = messages == null ? new ArrayList<>() : new ArrayList<>(messages);
            return this;
        }

        public Builder parameters(ChatRequestParameters parameters) {
            this.parameters = parameters;
            return this;
        }

        public Builder toolSpecifications(List<ToolSpecification> toolSpecifications) {
            this.toolSpecifications = toolSpecifications == null ? new ArrayList<>() : new ArrayList<>(toolSpecifications);
            return this;
        }

        public ChatRequest build() {
            return new ChatRequest(messages, parameters, toolSpecifications);
        }
    }
}

