package org.yuca.ai.service;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolSpecification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yuca.ai.config.LangChain4jProperties;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * LangChain4j 服务实现类
 * 基于LangChain4j框架提供AI能力
 */
@Slf4j
@Service
public class LangChain4jServiceImpl implements LangChain4jService {

    @Autowired
    private Map<String, ChatLanguageModel> chatModels;

    @Autowired
    private Map<String, StreamingChatLanguageModel> streamingModels;

    @Autowired
    private LangChain4jProperties properties;

    /**
     * 会话记忆管理
     * Key: sessionId, Value: List<ChatMessage>
     */
    private final Map<String, List<ChatMessage>> memories = new ConcurrentHashMap<>();

    @Override
    public String chat(String provider, String sessionId, String userMessage) {
        ChatLanguageModel model = getChatModel(provider);
        List<ChatMessage> memory = getMemory(sessionId);

        // 添加用户消息到记忆
        memory.add(new UserMessage(userMessage));

        // 调用模型
        Response<AiMessage> response = model.generate(memory);

        // 添加AI响应到记忆
        AiMessage aiMessage = response.content();
        memory.add(aiMessage);

        log.info("Chat response from provider: {}, sessionId: {}, tokens: {}",
                provider, sessionId, response.tokenUsage());

        return aiMessage.text();
    }

    @Override
    public void chatStream(String provider, String sessionId, String userMessage, Consumer<String> tokenHandler) {
        StreamingChatLanguageModel model = getStreamingModel(provider);
        List<ChatMessage> memory = getMemory(sessionId);

        // 添加用户消息到记忆
        memory.add(new UserMessage(userMessage));

        StringBuilder fullResponse = new StringBuilder();

        // 流式调用
        model.generate(memory, (token) -> {
            String content = token.content();
            if (content != null && !content.isEmpty()) {
                tokenHandler.accept(content);
                fullResponse.append(content);
            }
        });

        // 添加完整响应到记忆
        memory.add(new AiMessage(fullResponse.toString()));

        log.info("Chat stream completed from provider: {}, sessionId: {}", provider, sessionId);
    }

    @Override
    public Response<AiMessage> chatStreamWithResponse(String provider, String sessionId,
                                                      String userMessage, Consumer<String> tokenHandler) {
        StreamingChatLanguageModel model = getStreamingModel(provider);
        List<ChatMessage> memory = getMemory(sessionId);

        // 添加用户消息到记忆
        memory.add(new UserMessage(userMessage));

        StringBuilder fullResponse = new StringBuilder();

        // 使用自定义处理器来捕获完整响应
        Response.Builder<AiMessage> responseBuilder = Response.builder();

        model.generate(memory,
                (token) -> {
                    String content = token.content();
                    if (content != null && !content.isEmpty()) {
                        tokenHandler.accept(content);
                        fullResponse.append(content);
                    }
                },
                responseBuilder
        );

        // 添加完整响应到记忆
        AiMessage aiMessage = new AiMessage(fullResponse.toString());
        memory.add(aiMessage);

        Response<AiMessage> response = responseBuilder.content(aiMessage).build();

        log.info("Chat stream with response completed from provider: {}, sessionId: {}, tokens: {}",
                provider, sessionId, response.tokenUsage());

        return response;
    }

    @Override
    public String chatWithTools(String provider, String sessionId, String userMessage, List<Object> tools) {
        ChatLanguageModel model = getChatModel(provider);
        List<ChatMessage> memory = getMemory(sessionId);

        // 添加用户消息到记忆
        memory.add(new UserMessage(userMessage));

        // 将工具转换为 ToolSpecification
        List<ToolSpecification> toolSpecifications = convertToolsToSpecifications(tools);

        // 调用模型（带工具）
        Response<AiMessage> response = model.generate(memory, toolSpecifications);
        AiMessage aiMessage = response.content();

        // 处理工具调用
        if (aiMessage.hasToolExecutionRequests()) {
            List<ChatMessage> toolExecutionMessages = new ArrayList<>();
            toolExecutionMessages.add(aiMessage);

            // 执行每个工具调用
            for (dev.langchain4j.agent.tool.ToolExecutionRequest request : aiMessage.toolExecutionRequests()) {
                try {
                    Object result = executeTool(request, tools);
                    String resultAsString = result != null ? result.toString() : "null";

                    toolExecutionMessages.add(new dev.langchain4j.data.message.ToolExecutionResultMessage(
                            request.id(),
                            request.name(),
                            resultAsString
                    ));

                    log.info("Executed tool: {}, result: {}", request.name(), resultAsString);
                } catch (Exception e) {
                    log.error("Error executing tool: {}", request.name(), e);
                    toolExecutionMessages.add(new dev.langchain4j.data.message.ToolExecutionResultMessage(
                            request.id(),
                            request.name(),
                            "Error: " + e.getMessage()
                    ));
                }
            }

            // 将工具执行结果添加到记忆
            memory.addAll(toolExecutionMessages);

            // 再次调用模型获取最终响应
            Response<AiMessage> finalResponse = model.generate(memory);
            AiMessage finalAiMessage = finalResponse.content();
            memory.add(finalAiMessage);

            log.info("Chat with tools completed from provider: {}, sessionId: {}, tokens: {}",
                    provider, sessionId, finalResponse.tokenUsage());

            return finalAiMessage.text();
        } else {
            // 没有工具调用，直接返回响应
            memory.add(aiMessage);
            return aiMessage.text();
        }
    }

    @Override
    public ChatLanguageModel getChatModel(String provider) {
        ChatLanguageModel model = chatModels.get(provider.toLowerCase());
        if (model == null) {
            throw new IllegalArgumentException("Unknown provider: " + provider +
                    ". Available providers: " + chatModels.keySet());
        }
        return model;
    }

    @Override
    public StreamingChatLanguageModel getStreamingModel(String provider) {
        StreamingChatLanguageModel model = streamingModels.get(provider.toLowerCase());
        if (model == null) {
            throw new IllegalArgumentException("Unknown provider: " + provider +
                    ". Available providers: " + streamingModels.keySet());
        }
        return model;
    }

    @Override
    public void clearMemory(String sessionId) {
        memories.remove(sessionId);
        log.info("Cleared memory for session: {}", sessionId);
    }

    @Override
    public List<ChatMessage> getChatHistory(String sessionId) {
        return new ArrayList<>(getMemory(sessionId));
    }

    @Override
    public boolean isModelAvailable(String provider) {
        return chatModels.containsKey(provider.toLowerCase());
    }

    /**
     * 获取会话记忆
     */
    private List<ChatMessage> getMemory(String sessionId) {
        return memories.computeIfAbsent(sessionId, id -> {
            // 可以初始化系统提示词
            List<ChatMessage> messages = new ArrayList<>();
            // messages.add(new SystemMessage("You are a helpful AI assistant."));
            return messages;
        });
    }

    /**
     * 将工具对象转换为 ToolSpecification 列表
     */
    private List<ToolSpecification> convertToolsToSpecifications(List<Object> tools) {
        if (tools == null || tools.isEmpty()) {
            return Collections.emptyList();
        }

        // 使用 LangChain4j 的工具提供机制
        // 这里简化处理，实际应该通过 ToolProvider 来获取
        return tools.stream()
                .flatMap(tool -> {
                    try {
                        // 使用反射或 LangChain4j 的工具解析机制
                        // 这里返回空列表，实际实现需要使用 LangChain4j 的 ToolProvider
                        return Collections.<ToolSpecification>emptyList().stream();
                    } catch (Exception e) {
                        log.error("Error converting tool to specification: {}", tool.getClass().getName(), e);
                        return Collections.<ToolSpecification>emptyList().stream();
                    }
                })
                .collect(Collectors.toList());
    }

    /**
     * 执行工具调用
     */
    private Object executeTool(dev.langchain4j.agent.tool.ToolExecutionRequest request, List<Object> tools) {
        for (Object tool : tools) {
            // 查找匹配的工具并执行
            // 这里需要通过反射或工具注册机制来实现
            // 简化实现：直接返回示例结果
            log.warn("Tool execution not fully implemented yet for tool: {}", request.name());
            return "Tool execution result for: " + request.name();
        }

        throw new IllegalArgumentException("Tool not found: " + request.name());
    }
}
