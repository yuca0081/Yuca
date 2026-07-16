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

    /** 单次工具调用最大尝试次数（含首次调用，共 3 次机会） */
    private static final int MAX_TOOL_ATTEMPTS = 3;
    /** 压缩后的错误 message 最大长度，超出按首部根因截断 */
    private static final int MAX_ERROR_MESSAGE_LENGTH = 200;

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
     * 执行工具：最多 {@value MAX_TOOL_ATTEMPTS} 次尝试；全部失败时压缩错误为精炼描述返回。
     *
     * <p>调用方（{@link #execute}）会把返回值拼进消息列表回塞给 LLM，LLM 据此自纠正参数或改换工具。
     * 因此失败路径返回的字符串必须简洁可读——原始异常 message 可能含完整堆栈、HTTP 响应体、JSON dump
     * 等冗长内容，直接回塞既爆 token 又干扰决策，走 {@link #compressToolError} 压缩。
     */
    private String executeTool(ToolExecutionRequest request) {
        ToolExecutor executor = toolExecutors.get(request.name());
        if (executor == null) {
            log.warn("工具不存在: {}", request.name());
            return "[工具不存在，请确认工具名] 期望: " + request.name();
        }

        Exception lastException = null;
        for (int attempt = 1; attempt <= MAX_TOOL_ATTEMPTS; attempt++) {
            try {
                String result = executor.execute(request);
                if (attempt == 1) {
                    log.info("工具执行成功: {} = {}", request.name(), result);
                } else {
                    log.info("工具执行成功（第 {} 次尝试命中）: {}", attempt, request.name());
                }
                return result;
            } catch (Exception e) {
                lastException = e;
                if (attempt < MAX_TOOL_ATTEMPTS) {
                    log.warn("工具执行失败（尝试 {}/{}）: {} - {}，将重试",
                            attempt, MAX_TOOL_ATTEMPTS, request.name(), e.getMessage());
                } else {
                    log.error("工具 {} 共 {} 次尝试均失败", request.name(), MAX_TOOL_ATTEMPTS, e);
                }
            }
        }

        return compressToolError(lastException);
    }

    /**
     * 压缩工具异常为精炼描述。策略：
     * <ol>
     *   <li>保留异常类型 simpleName（去掉包前缀，如 SocketTimeoutException）</li>
     *   <li>折叠所有空白为单个空格，消除换行/缩进噪声</li>
     *   <li>超长 message 截断保留首部根因</li>
     * </ol>
     * 最终格式：{@code [共尝试 N 次均失败，请修正参数或换工具] ExceptionType: <精简 message>}
     */
    private String compressToolError(Exception e) {
        String errorType = e.getClass().getSimpleName();
        String raw = e.getMessage();
        String concise = (raw == null || raw.isBlank())
                ? "无详细错误信息"
                : raw.replaceAll("\\s+", " ").trim();
        if (concise.length() > MAX_ERROR_MESSAGE_LENGTH) {
            concise = concise.substring(0, MAX_ERROR_MESSAGE_LENGTH) + "...";
        }
        return String.format("[共尝试 %d 次均失败，请修正参数或换工具] %s: %s",
                MAX_TOOL_ATTEMPTS, errorType, concise);
    }
}
