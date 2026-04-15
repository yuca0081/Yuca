package org.yuca.ai.agent;

import dev.langchain4j.model.chat.ChatModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Agent 构建器
 */
public class AgentBuilder {

    private ChatModel chatModel;
    private final List<ChatInterceptor> interceptors = new ArrayList<>();
    private ToolManager toolManager;
    private int maxToolRounds = 5;

    public AgentBuilder chatModel(ChatModel chatModel) {
        this.chatModel = chatModel;
        return this;
    }

    public AgentBuilder interceptor(ChatInterceptor interceptor) {
        this.interceptors.add(interceptor);
        return this;
    }

    public AgentBuilder interceptors(List<ChatInterceptor> interceptors) {
        this.interceptors.addAll(interceptors);
        return this;
    }

    public AgentBuilder toolManager(ToolManager toolManager) {
        this.toolManager = toolManager;
        return this;
    }

    public AgentBuilder maxToolRounds(int maxToolRounds) {
        this.maxToolRounds = maxToolRounds;
        return this;
    }

    public Agent build() {
        return new Agent(chatModel, List.copyOf(interceptors), toolManager, maxToolRounds);
    }
}
