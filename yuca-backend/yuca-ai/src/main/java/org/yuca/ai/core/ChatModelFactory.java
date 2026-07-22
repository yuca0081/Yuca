package org.yuca.ai.core;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.yuca.ai.config.AiProperties;
import org.yuca.ai.core.model.ChatModel;
import org.yuca.ai.core.model.StreamingChatModel;
import org.yuca.ai.core.provider.qwen.QwenChatModel;
import org.yuca.ai.core.provider.qwen.QwenStreamingChatModel;

/**
 * 统一的 ChatModel 构造工厂。
 *
 * <p>目的：
 * <ul>
 *   <li>统一 LLM 客户端构造入口，避免散落各处的 {@code new QwenChatModel(...)}</li>
 *   <li>打破 {@code AgentFactory} 与各业务服务（如 {@code MultiQueryExpander}）之间
 *       经由 {@code RetrievalService} 形成的循环依赖</li>
 *   <li>满足「新增 LLM 调用一律走工厂，不直接 new」的约束（CLAUDE.md）</li>
 * </ul>
 *
 * <p>当前仅支持 Qwen（DashScope）provider；后续接入 OpenAI 兼容端点或 MCP-hosted
 * 模型时，在此扩展 {@code provider} 维度即可。
 *
 * <p>使用场景：
 * <ul>
 *   <li>{@link AgentFactory} 构造主 Agent 与副任务（摘要 / 意图识别）模型</li>
 *   <li>各业务模块（{@code MultiQueryExpander} 等）需要轻量 LLM 调用时</li>
 * </ul>
 *
 * @author Yuca
 * @since 2026-07-22
 */
@Component
@RequiredArgsConstructor
public class ChatModelFactory {

    private final AiProperties aiProperties;

    /**
     * 默认模型 ChatModel（使用 {@code yuca.ai.dashscope.model-name} 配置的主模型）。
     */
    public ChatModel chatModel() {
        return chatModel(aiProperties.getDashscope().getModelName());
    }

    /**
     * 指定模型名构造 ChatModel，复用 dashscope 的 base-url 与 api-key。
     *
     * <p>用于副任务（摘要、意图识别、查询扩展、标题生成等）切换到 qwen-turbo 等
     * 便宜模型，避免占用主模型配额。
     */
    public ChatModel chatModel(String modelName) {
        AiProperties.ProviderConfig dashscope = aiProperties.getDashscope();
        return new QwenChatModel(dashscope.getBaseUrl(), dashscope.getApiKey(), modelName);
    }

    /**
     * 流式 ChatModel（SSE 场景）。当前固定使用主模型。
     */
    public StreamingChatModel streamingChatModel() {
        AiProperties.ProviderConfig dashscope = aiProperties.getDashscope();
        return new QwenStreamingChatModel(
                dashscope.getBaseUrl(), dashscope.getApiKey(), dashscope.getModelName());
    }
}
