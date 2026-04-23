package org.yuca.ai.client;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.output.TokenUsage;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.yuca.ai.agent.Agent;
import org.yuca.ai.agent.ChatContext;
import org.yuca.ai.agent.enhancer.HistoryEnhancer;
import org.yuca.ai.agent.enhancer.SystemPromptEnhancer;
import org.yuca.ai.config.AiProperties;
import org.yuca.ai.history.ChatHistoryStore;
import org.yuca.ai.skill.SkillRegistry;
import org.yuca.ai.tool.ToolManager;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.List;
import java.util.function.Consumer;

/**
 * AI客户端
 * 供其他模块使用的统一AI能力入口
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiClient {

    private final ChatModel chatModel;
    private final StreamingChatModel streamingChatModel;
    private final ChatHistoryStore historyStore;
    private final AiProperties aiProperties;
    private final ToolManager toolManager;
    private final SkillRegistry skillRegistry;

    private Agent historyAgent;
    private Agent defaultAgent;

    @PostConstruct
    void init() {
        defaultAgent = Agent.builder()
                .chatModel(chatModel)
                .build();

        String systemPrompt = buildSystemPrompt();
        historyAgent = Agent.builder()
                .chatModel(chatModel)
                .enhancer(new SystemPromptEnhancer(systemPrompt))
                .enhancer(new HistoryEnhancer(historyStore, aiProperties.getMemory().getMaxMessages()))
                .toolManager(toolManager)
                .build();
    }

    /**
     * 构建 system prompt，包含 skill 元数据清单
     */
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

    /**
     * 同步对话（无历史，适用于一次性调用如生成标题等）
     *
     * @param message 用户消息
     * @return AI回复文本
     */
    public String chat(String message) {
        ChatRequest request = ChatRequest.builder()
                .messages(List.of(UserMessage.from(message)))
                .build();
        ChatResponse response = defaultAgent.execute(request, new ChatContext());
        return response.aiMessage().text();
    }

    /**
     * 同步对话（带对话历史）
     *
     * @param message   用户消息
     * @param sessionId 会话ID，用于加载和保存对话历史
     * @return AI回复文本
     */
    public String chat(String message, String sessionId) {
        ChatContext context = new ChatContext();
        context.setSessionId(sessionId);

        ChatRequest request = ChatRequest.builder()
                .messages(List.of(UserMessage.from(message)))
                .build();

        ChatResponse response = historyAgent.execute(request, context);
        return response.aiMessage().text();
    }

    /**
     * 流式对话
     *
     * @param request          聊天请求
     * @param tokenConsumer    token回调（每个token触发一次）
     * @param responseConsumer 完成回调（流式结束时触发，含token统计）
     * @return Flux<String> token流
     */
    public Flux<String> streamChat(ChatRequest request,
                                   Consumer<String> tokenConsumer,
                                   Consumer<ChatResponse> responseConsumer) {
        Sinks.Many<String> sink = Sinks.many().multicast().onBackpressureBuffer();

        streamingChatModel.chat(request, new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String partialResponse) {
                if (partialResponse != null && !partialResponse.isEmpty()) {
                    sink.tryEmitNext(partialResponse);
                    if (tokenConsumer != null) tokenConsumer.accept(partialResponse);
                }
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                if (responseConsumer != null) responseConsumer.accept(completeResponse);
                sink.tryEmitComplete();
            }

            @Override
            public void onError(Throwable error) {
                log.error("流式输出错误", error);
                sink.tryEmitError(error);
            }
        });

        return sink.asFlux();
    }

    /**
     * Agent对话（支持增强器链 + 工具调用）
     *
     * @param request 聊天请求
     * @param context 上下文（sessionId、userId等）
     * @return ChatResponse
     */
    public ChatResponse agent(ChatRequest request, ChatContext context) {
        return defaultAgent.execute(request, context);
    }
}
