package org.yuca.ai.agent;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * Agent 执行引擎
 * 拦截器链 + 工具调用循环
 */
@Slf4j
public class Agent {

    private final ChatModel chatModel;
    private final List<ChatEnhancer> enhancers;
    private final ToolManager toolManager;
    private final int maxToolRounds;

    Agent(ChatModel chatModel, List<ChatEnhancer> enhancers,
          ToolManager toolManager, int maxToolRounds) {
        this.chatModel = chatModel;
        this.enhancers = enhancers;
        this.toolManager = toolManager;
        this.maxToolRounds = maxToolRounds;
    }

    public static AgentBuilder builder() {
        return new AgentBuilder();
    }

    /**
     * 执行 Agent：拦截器链 → 工具调用循环 → 拦截器链（反序）
     */
    public ChatResponse execute(ChatRequest request, ChatContext context) {
        // 1. 增强器链（注入上下文）
        for (ChatEnhancer enhancer : enhancers) {
            request = enhancer.before(request, context);
        }

        // 2. 工具调用循环
        List<ChatMessage> messages = new ArrayList<>(request.messages());
        ChatResponse response = null;

        for (int round = 0; round < maxToolRounds; round++) {
            var reqBuilder = ChatRequest.builder()
                    .messages(messages);

            // 如果有工具，附加工具 schema
            if (toolManager != null && !toolManager.specifications().isEmpty()) {
                reqBuilder.toolSpecifications(toolManager.specifications());
            }

            response = chatModel.chat(reqBuilder.build());

            // 不需要调用工具，结束循环
            if (response.aiMessage() == null
                    || !response.aiMessage().hasToolExecutionRequests()) {
                break;
            }

            // 执行工具调用
            messages.add(response.aiMessage());
            for (ToolExecutionRequest toolReq : response.aiMessage().toolExecutionRequests()) {
                String result = toolManager.execute(toolReq);
                messages.add(ToolExecutionResultMessage.from(toolReq, result));
            }

            log.debug("工具调用轮次 {} 完成", round + 1);
        }

        // 存储完整对话（含工具调用中间过程）供增强器使用
        List<ChatMessage> fullConversation = new ArrayList<>(messages);
        if (response != null && response.aiMessage() != null) {
            fullConversation.add(response.aiMessage());
        }
        context.attribute("_agentConversation", fullConversation);

        // 3. 增强器链（反序 after）
        for (int i = enhancers.size() - 1; i >= 0; i--) {
            enhancers.get(i).after(response, context);
        }

        return response;
    }
}
