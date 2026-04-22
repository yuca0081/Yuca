package org.yuca.ai.tool;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

/**
 * 工具管理器
 * 扫描 @Tool 注解注册工具，解析 ToolExecutionRequest 执行工具
 */
@Slf4j
public class ToolManager {

    private final Map<String, ToolEntry> tools = new LinkedHashMap<>();
    private final Gson gson = new Gson();

    /**
     * 注册一个工具 Bean，扫描 @Tool 注解
     */
    public ToolManager register(Object toolBean) {
        for (Method method : toolBean.getClass().getDeclaredMethods()) {
            Tool toolAnnotation = method.getAnnotation(Tool.class);
            if (toolAnnotation == null) continue;

            method.setAccessible(true);
            String name = method.getName();
            String[] descriptions = toolAnnotation.value();
            String description = (descriptions != null && descriptions.length > 0)
                    ? descriptions[0] : name;

            ToolSpecification spec = ToolSpecification.builder()
                    .name(name)
                    .description(description)
                    .build();

            tools.put(name, new ToolEntry(method, toolBean, spec));
            log.info("注册工具: {}", name);
        }
        return this;
    }

    /**
     * 获取所有已注册工具的 schema
     */
    public List<ToolSpecification> specifications() {
        return tools.values().stream()
                .map(ToolEntry::specification)
                .toList();
    }

    /**
     * 按 allowedNames 过滤，返回子集 schema
     */
    public List<ToolSpecification> specifications(Set<String> allowedNames) {
        if (allowedNames == null || allowedNames.isEmpty()) {
            return List.of();
        }
        return tools.entrySet().stream()
                .filter(e -> allowedNames.contains(e.getKey()))
                .map(e -> e.getValue().specification())
                .toList();
    }

    /**
     * 创建一个只包含指定工具的子 ToolManager
     */
    public ToolManager subset(Set<String> names) {
        if (names == null || names.isEmpty() || names.size() == tools.size()) {
            return this;
        }
        ToolManager subset = new ToolManager();
        for (Map.Entry<String, ToolEntry> entry : tools.entrySet()) {
            if (names.contains(entry.getKey())) {
                subset.tools.put(entry.getKey(), entry.getValue());
            }
        }
        return subset;
    }

    /**
     * 执行工具：解析 JSON 参数 → 反射调用 → 返回结果字符串
     */
    public String execute(ToolExecutionRequest request) {
        ToolEntry entry = tools.get(request.name());
        if (entry == null) {
            log.warn("未找到工具: {}", request.name());
            return "工具不存在: " + request.name();
        }

        try {
            Map<String, Object> args = gson.fromJson(
                    request.arguments(),
                    new TypeToken<Map<String, Object>>() {}.getType()
            );

            Object[] methodArgs = buildMethodArgs(entry.method, args);
            Object result = entry.method.invoke(entry.bean, methodArgs);

            String resultStr = result != null ? gson.toJson(result) : "null";
            log.info("工具执行成功: {}({}) = {}", request.name(), args, resultStr);
            return resultStr;

        } catch (Exception e) {
            log.error("工具执行失败: {}", request.name(), e);
            return "工具执行失败: " + e.getMessage();
        }
    }

    private Object[] buildMethodArgs(Method method, Map<String, Object> args) {
        Parameter[] parameters = method.getParameters();
        Object[] methodArgs = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            String paramName = parameters[i].getName();
            Object value = args.get(paramName);
            methodArgs[i] = convertType(value, parameters[i].getType());
        }
        return methodArgs;
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

    private record ToolEntry(Method method, Object bean, ToolSpecification specification) {}
}
