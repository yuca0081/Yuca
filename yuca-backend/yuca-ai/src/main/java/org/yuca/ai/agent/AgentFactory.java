package org.yuca.ai.agent;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.yuca.ai.agent.enhancer.ChatEnhancer;
import org.yuca.ai.agent.enhancer.HistoryEnhancer;
import org.yuca.ai.agent.enhancer.IntentRecognitionEnhancer;
import org.yuca.ai.agent.enhancer.RagEnhancer;
import org.yuca.ai.agent.enhancer.SummaryEnhancer;
import org.yuca.ai.agent.enhancer.SystemPromptEnhancer;
import org.yuca.ai.config.AiProperties;
import org.yuca.ai.core.ChatModelFactory;
import org.yuca.ai.core.model.ChatModel;
import org.yuca.ai.core.model.StreamingChatModel;
import org.yuca.ai.history.ChatHistoryStore;
import org.yuca.ai.retrieval.RetrievalService;
import org.yuca.ai.retrieval.TokenBudgetAssembler;
import org.yuca.ai.skill.SkillRegistry;
import org.yuca.ai.tool.Calculator;
import org.yuca.ai.tool.SkillTool;
import org.yuca.ai.tool.ToolExtractor;

import java.util.ArrayList;
import java.util.List;

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
    private final RetrievalService retrievalService;
    private final TokenBudgetAssembler tokenBudgetAssembler;
    private final ChatModelFactory chatModelFactory;

    /**
     * 创建简单 Agent（无历史、无工具、无增强器）
     * 适用于单轮对话，如生成标题等
     */
    public Agent simpleAgent() {
        return simpleAgent(new ChatContext(), null);
    }

    public Agent simpleAgent(ChatContext context) {
        return simpleAgent(context, null);
    }

    /**
     * 创建简单 Agent（可指定模型），用于成本敏感的批量单轮场景。
     *
     * <p>典型用法：章节摘要生成（{@code simpleAgent("qwen-turbo")}）、文本分类、
     * 占位标题生成等"输入 prompt → 输出文本"的一次性任务。
     *
     * @param context   请求上下文，通常用 {@code new ChatContext()}
     * @param modelName 模型名（如 "qwen-turbo"），null 时退回到默认模型
     */
    public Agent simpleAgent(ChatContext context, String modelName) {
        ChatModel model = (modelName == null) ? chatModelFactory.chatModel() : chatModelFactory.chatModel(modelName);
        return Agent.builder()
                .chatModel(model)
                .context(context)
                .build();
    }

    /**
     * 便捷重载：单轮 Agent + 指定模型 + 默认上下文。
     */
    public Agent simpleAgent(String modelName) {
        return simpleAgent(new ChatContext(), modelName);
    }



    /**
     * 创建默认 Agent（带历史记忆、系统提示、工具调用）
     * 适用于多轮对话场景
     */
    public Agent defaultAgent() {
        return defaultAgent(new ChatContext());
    }

    public Agent defaultAgent(ChatContext context) {
        return defaultAgent(context, null);
    }

    /**
     * 创建默认 Agent（带历史记忆、系统提示、工具调用）
     * 可选启用 RAG 知识库检索
     *
     * @param context 请求上下文
     * @param kbId    知识库ID，为 null 时不启用 RAG
     */
    public Agent defaultAgent(ChatContext context, Long kbId) {
        String systemPrompt = buildSystemPrompt();

        List<ChatEnhancer> enhancers = new ArrayList<>();

        // 历史摘要压缩（order=-2，比意图识别更先跑）：触发时把最早一批消息压成 SUMMARY 持久化，
        // HistoryEnhancer 接下来通过 getActiveMessages 只加载 [SUMMARY, ...最近 raw]
        if (aiProperties.getSummary().isEnabled()) {
            ChatModel summaryModel = chatModelFactory.chatModel(aiProperties.getSummary().getModelName());
            enhancers.add(new SummaryEnhancer(historyStore, aiProperties.getSummary(), summaryModel));
        }

        // 意图识别（order=-1，最先跑）：写 context.intent 供 RagEnhancer 路由
        // 失败/未启用时 RagEnhancer 走原路径，行为等价于重构前
        if (aiProperties.getIntentClassifier().isEnabled()) {
            ChatModel intentModel = chatModelFactory.chatModel(aiProperties.getIntentClassifier().getModelName());
            enhancers.add(new IntentRecognitionEnhancer(intentModel));
        }

        if (kbId != null) {
            enhancers.add(new RagEnhancer(retrievalService, tokenBudgetAssembler, kbId, 5));
        }
        enhancers.add(new SystemPromptEnhancer(systemPrompt));
        enhancers.add(new HistoryEnhancer(historyStore, 50));

        // 从工具对象提取 ToolSpecification 和 ToolExecutor
        ToolExtractor toolExtractor = new ToolExtractor(List.of(new Calculator(), skillTool));

        return Agent.builder()
                .chatModel(chatModelFactory.chatModel())
                .context(context)
                .enhancers(enhancers)
                .toolSpecifications(toolExtractor.getSpecifications())
                .toolExecutors(toolExtractor.getExecutors())
                .build();
    }

    // =============================================================

    /**
     * 流式 ChatModel 门面方法——委托 {@link ChatModelFactory}。
     *
     * <p>保留 public 是为了 {@code AiClient} 等历史调用方不破坏二进制兼容；
     * 新代码请直接注入 {@code ChatModelFactory}。
     */
    public StreamingChatModel buildStreamingChatModel() {
        return chatModelFactory.streamingChatModel();
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
