package org.yuca.ai.agent;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.community.model.dashscope.QwenStreamingChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.tool.DefaultToolExecutor;
import dev.langchain4j.service.tool.ToolExecutor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.yuca.ai.agent.enhancer.ChatEnhancer;
import org.yuca.ai.agent.enhancer.HistoryEnhancer;
import org.yuca.ai.agent.enhancer.SystemPromptEnhancer;
import org.yuca.ai.config.AiProperties;
import org.yuca.ai.history.ChatHistoryStore;
import org.yuca.ai.skill.SkillRegistry;
import org.yuca.ai.tool.Calculator;
import org.yuca.ai.tool.SkillTool;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Agent 工厂
 * 持有共享组件（enhancers、toolObjects），按需创建 Agent 实例
 */
@Component
@RequiredArgsConstructor
public class AgentFactory {

    private final ChatHistoryStore historyStore;
    private final AiProperties aiProperties;
    private final SkillRegistry skillRegistry;
    private final SkillTool skillTool;

    /**
     * 创建简单 Agent（无历史、无工具、无增强器）
     * 适用于单轮对话，如生成标题等
     */
    public Agent simpleAgent() {
        return simpleAgent(new ChatContext());
    }

    public Agent simpleAgent(ChatContext context) {
        return Agent.builder()
                .chatModel(buildChatModel())
                .context(context)
                .build();
    }



    /**
     * 创建默认 Agent（带历史记忆、系统提示、工具调用）
     * 适用于多轮对话场景
     */
    public Agent defaultAgent() {
        return defaultAgent(new ChatContext());
    }

    public Agent defaultAgent(ChatContext context) {
        String systemPrompt = buildSystemPrompt();
        List<ChatEnhancer> enhancers = List.of(
                new SystemPromptEnhancer(systemPrompt),
                new HistoryEnhancer(historyStore, 50)
        );

        // 工具对象
        List<Object> toolObjects = List.of(new Calculator(), skillTool);

        // 从工具对象提取 ToolSpecification 和 ToolExecutor
        List<ToolSpecification> specs = new ArrayList<>();
        Map<String, ToolExecutor> executors = new HashMap<>();
        for (Object toolObject : toolObjects) {
            specs.addAll(ToolSpecifications.toolSpecificationsFrom(toolObject));
            for (Method method : toolObject.getClass().getDeclaredMethods()) {
                if (method.isAnnotationPresent(Tool.class)) {
                    executors.put(method.getName(), new DefaultToolExecutor(toolObject, method));
                }
            }
        }

        return Agent.builder()
                .chatModel(buildChatModel())
                .context(context)
                .enhancers(enhancers)
                .toolSpecifications(specs)
                .toolExecutors(executors)
                .build();
    }

    // =============================================================

    private ChatModel buildChatModel() {
        AiProperties.ProviderConfig dashscope = aiProperties.getDashscope();
        return QwenChatModel.builder()
                .modelName(dashscope.getModelName())
                .apiKey(dashscope.getApiKey())
                .build();
    }

    public StreamingChatModel buildStreamingChatModel() {
        AiProperties.ProviderConfig dashscope = aiProperties.getDashscope();
        return QwenStreamingChatModel.builder()
                .modelName(dashscope.getModelName())
                .apiKey(dashscope.getApiKey())
                .build();
    }

    private String buildSystemPrompt() {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一个友好的AI助手，能够记住之前的对话内容。请用简洁、准确的方式回答用户的问题。\n");

        var skills = skillRegistry.getAllSkills();
        if (!skills.isEmpty()) {
            sb.append("\n## 可用技能\n");
            sb.append("当用户请求匹配以下技能时，请调用 executeSkill 工具执行对应技能：\n\n");
            for (var skill : skills) {
                sb.append("- **").append(skill.getName()).append("**: ")
                        .append(skill.getDescription()).append("\n");
            }
        }

        return sb.toString();
    }
}
