package org.yuca.ai.agent;

import dev.langchain4j.model.chat.ChatModel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Agent 构建器
 */
public class AgentBuilder {

    private ChatModel chatModel;
    private final List<ChatEnhancer> enhancers = new ArrayList<>();
    private ToolManager toolManager;
    private int maxToolRounds = 5;

    public AgentBuilder chatModel(ChatModel chatModel) {
        this.chatModel = chatModel;
        return this;
    }

    public AgentBuilder enhancer(ChatEnhancer enhancer) {
        this.enhancers.add(enhancer);
        return this;
    }

    public AgentBuilder enhancers(List<ChatEnhancer> enhancers) {
        this.enhancers.addAll(enhancers);
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
        return new Agent(chatModel, enhancers.stream()
                .sorted(Comparator.comparingInt(ChatEnhancer::order))
                .toList(), toolManager, maxToolRounds);
    }
}
