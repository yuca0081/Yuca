package org.yuca.ai.agent;

import lombok.extern.slf4j.Slf4j;
import org.yuca.ai.agent.enhancer.ChatEnhancer;
import org.yuca.ai.core.message.ChatMessage;
import org.yuca.ai.core.message.UserMessage;
import org.yuca.ai.core.model.ChatModel;
import org.yuca.ai.core.model.ChatRequest;
import org.yuca.ai.core.model.ChatResponse;
import org.yuca.ai.core.tool.ToolExecutionRequest;
import org.yuca.ai.core.tool.ToolExecutor;
import org.yuca.ai.core.tool.ToolSpecification;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Agent 执行引擎
 * 增强器链 + 工具调用循环
 */
@Slf4j
public class Agent {

    private final ChatModel chatModel;
    private final ChatContext context;
    private final List<ChatEnhancer> enhancers;
    private final List<ToolSpecification> toolSpecifications;
    private final Map<String, ToolExecutor> toolExecutors;
    private final int maxToolRounds;

    Agent(ChatModel chatModel, ChatContext context, List<ChatEnhancer> enhancers,
          List<ToolSpecification> toolSpecifications, Map<String, ToolExecutor> toolExecutors, int maxToolRounds) {
        this.chatModel = chatModel;
        this.context = context;
        this.enhancers = enhancers;
        this.toolSpecifications = toolSpecifications;
        this.toolExecutors = toolExecutors;
        this.maxToolRounds = maxToolRounds;
    }

    public static AgentBuilder builder() {
        return new AgentBuilder();
    }

    /**
     * 执行 Agent：增强器链 → 工具调用循环 → 增强器链（反序）
     */
    public ChatResponse execute(ChatRequest request) {
        // 1. 增强器链（注入上下文）
        for (ChatEnhancer enhancer : enhancers) {
            request = enhancer.before(request, context);
        }

        // 2. 工具调用循环
        List<ChatMessage> messages = new ArrayList<>(request.messages());
        ChatResponse response = null;

        for (int round = 0; round < maxToolRounds; round++) {
            ChatRequest.Builder reqBuilder = ChatRequest.builder()
                    .messages(messages);

            if (toolSpecifications != null && !toolSpecifications.isEmpty()) {
                reqBuilder.toolSpecifications(toolSpecifications);
            }

            response = chatModel.chat(reqBuilder.build());

            if (response.aiMessage() == null || !response.aiMessage().hasToolExecutionRequests()) {
                break;
            }

            // 执行工具调用，将结果转为 UserMessage（兼容 Qwen/DashScope）
            StringBuilder resultsBuilder = new StringBuilder();
            for (ToolExecutionRequest toolReq : response.aiMessage().toolExecutionRequests()) {
                String result = executeTool(toolReq);
                resultsBuilder.append("[").append(toolReq.name()).append("] ")
                        .append(result).append("\n");
            }

            String toolCallsSummary = response.aiMessage().toolExecutionRequests().stream()
                    .map(req -> "- " + req.name() + "(" + req.arguments() + ")")
                    .collect(Collectors.joining("\n"));

            messages.add(UserMessage.from(
                    "你调用了以下工具：\n" + toolCallsSummary +
                    "\n\n执行结果：\n" + resultsBuilder +
                    "\n请根据以上结果继续回答用户。"));

            log.debug("工具调用轮次 {} 完成", round + 1);
        }

        // 存储完整对话供增强器使用
        List<ChatMessage> fullConversation = new ArrayList<>(messages);
        if (response != null && response.aiMessage() != null
                && !response.aiMessage().hasToolExecutionRequests()) {
            fullConversation.add(response.aiMessage());
        }
        context.attribute("_agentConversation", fullConversation);

        // 3. 增强器链（反序 after）
        for (int i = enhancers.size() - 1; i >= 0; i--) {
            enhancers.get(i).after(response, context);
        }

        return response;
    }

    /**
     * 执行工具：通过预建的 ToolExecutor 索引直接调用
     */
    private String executeTool(ToolExecutionRequest request) {
        ToolExecutor executor = toolExecutors.get(request.name());
        if (executor == null) {
            return "工具不存在: " + request.name();
        }
        try {
            String result = executor.execute(request);
            log.info("工具执行成功: {} = {}", request.name(), result);
            return result;
        } catch (Exception e) {
            log.error("工具执行失败: {}", request.name(), e);
            return "工具执行失败: " + e.getMessage();
        }
    }
}
