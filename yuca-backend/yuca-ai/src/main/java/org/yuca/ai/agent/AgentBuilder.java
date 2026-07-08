package org.yuca.ai.agent;

import org.yuca.ai.agent.enhancer.ChatEnhancer;
import org.yuca.ai.core.model.ChatModel;
import org.yuca.ai.core.tool.ToolExecutor;
import org.yuca.ai.core.tool.ToolSpecification;

import java.util.*;

/**
 * Agent 构建器
 */
public class AgentBuilder {

    private ChatModel chatModel;
    private ChatContext context;
    private final List<ChatEnhancer> enhancers = new ArrayList<>();
    private final List<ToolSpecification> toolSpecifications = new ArrayList<>();
    private final Map<String, ToolExecutor> toolExecutors = new HashMap<>();
    private int maxToolRounds = 5;

    public AgentBuilder chatModel(ChatModel chatModel) {
        this.chatModel = chatModel;
        return this;
    }

    public AgentBuilder context(ChatContext context) {
        this.context = context;
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

    public AgentBuilder toolSpecification(ToolSpecification spec) {
        this.toolSpecifications.add(spec);
        return this;
    }

    public AgentBuilder toolSpecifications(List<ToolSpecification> specs) {
        this.toolSpecifications.addAll(specs);
        return this;
    }

    public AgentBuilder toolExecutor(String name, ToolExecutor executor) {
        this.toolExecutors.put(name, executor);
        return this;
    }

    public AgentBuilder toolExecutors(Map<String, ToolExecutor> executors) {
        this.toolExecutors.putAll(executors);
        return this;
    }

    public AgentBuilder maxToolRounds(int maxToolRounds) {
        this.maxToolRounds = maxToolRounds;
        return this;
    }

    public Agent build() {
        if (context == null) {
            context = new ChatContext();
        }
        return new Agent(chatModel, context,
                enhancers.stream()
                        .sorted(Comparator.comparingInt(ChatEnhancer::order))
                        .toList(),
                toolSpecifications,
                Collections.unmodifiableMap(toolExecutors),
                maxToolRounds);
    }
}
