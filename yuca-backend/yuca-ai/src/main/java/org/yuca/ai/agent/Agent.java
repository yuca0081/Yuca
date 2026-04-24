package org.yuca.ai.agent;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.extern.slf4j.Slf4j;
import org.yuca.ai.agent.enhancer.ChatEnhancer;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

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
    private final List<Object> toolObjects;
    private final int maxToolRounds;

    Agent(ChatModel chatModel, ChatContext context, List<ChatEnhancer> enhancers,
          List<ToolSpecification> toolSpecifications, List<Object> toolObjects, int maxToolRounds) {
        this.chatModel = chatModel;
        this.context = context;
        this.enhancers = enhancers;
        this.toolSpecifications = toolSpecifications;
        this.toolObjects = toolObjects;
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
            var reqBuilder = ChatRequest.builder()
                    .messages(messages);

            if (toolSpecifications != null && !toolSpecifications.isEmpty()) {
                reqBuilder.toolSpecifications(toolSpecifications);
            }

            response = chatModel.chat(reqBuilder.build());

            if (response.aiMessage() == null
                    || !response.aiMessage().hasToolExecutionRequests()) {
                break;
            }

            // 执行工具调用
            messages.add(response.aiMessage());
            for (ToolExecutionRequest toolReq : response.aiMessage().toolExecutionRequests()) {
                String result = executeTool(toolReq);
                messages.add(ToolExecutionResultMessage.from(toolReq, result));
            }

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
     * 执行工具：通过反射调用 @Tool 方法
     */
    private String executeTool(ToolExecutionRequest request) {
        if (toolObjects == null || toolObjects.isEmpty()) {
            return "无可用的工具";
        }

        for (Object toolObject : toolObjects) {
            for (Method method : toolObject.getClass().getDeclaredMethods()) {
                if (method.isAnnotationPresent(Tool.class) && method.getName().equals(request.name())) {
                    try {
                        method.setAccessible(true);
                        Object result = invokeToolMethod(method, toolObject, request.arguments());
                        String resultStr = result != null ? result.toString() : "null";
                        log.info("工具执行成功: {} = {}", request.name(), resultStr);
                        return resultStr;
                    } catch (Exception e) {
                        log.error("工具执行失败: {}", request.name(), e);
                        return "工具执行失败: " + e.getMessage();
                    }
                }
            }
        }

        return "工具不存在: " + request.name();
    }

    private Object invokeToolMethod(Method method, Object target, String argumentsJson) throws Exception {
        if (method.getParameterCount() == 0) {
            return method.invoke(target);
        }

        // 使用 Gson 解析参数
        var gson = new com.google.gson.Gson();
        var type = new com.google.gson.reflect.TypeToken<java.util.Map<String, Object>>() {}.getType();
        java.util.Map<String, Object> args = gson.fromJson(argumentsJson, type);

        java.lang.reflect.Parameter[] parameters = method.getParameters();
        Object[] methodArgs = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            Object value = args.get(parameters[i].getName());
            methodArgs[i] = convertType(value, parameters[i].getType());
        }

        return method.invoke(target, methodArgs);
    }

    private Object convertType(Object value, Class<?> targetType) {
        if (value == null) return null;
        if (targetType.isInstance(value)) return value;

        if (targetType == int.class || targetType == Integer.class)
            return ((Number) value).intValue();
        if (targetType == long.class || targetType == Long.class)
            return ((Number) value).longValue();
        if (targetType == double.class || targetType == Double.class)
            return ((Number) value).doubleValue();
        if (targetType == float.class || targetType == Float.class)
            return ((Number) value).floatValue();
        if (targetType == boolean.class || targetType == Boolean.class)
            return Boolean.valueOf(value.toString());
        if (targetType == String.class)
            return value.toString();

        return value;
    }
}
