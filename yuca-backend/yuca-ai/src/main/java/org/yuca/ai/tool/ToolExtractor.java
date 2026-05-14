package org.yuca.ai.tool;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.service.tool.DefaultToolExecutor;
import dev.langchain4j.service.tool.ToolExecutor;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工具提取器
 * 从工具对象列表中提取 ToolSpecification 和 ToolExecutor
 */
public class ToolExtractor {

    private final List<ToolSpecification> specifications = new ArrayList<>();
    private final Map<String, ToolExecutor> executors = new HashMap<>();

    public ToolExtractor(List<Object> toolObjects) {
        for (Object toolObject : toolObjects) {
            specifications.addAll(ToolSpecifications.toolSpecificationsFrom(toolObject));
            for (Method method : toolObject.getClass().getDeclaredMethods()) {
                if (method.isAnnotationPresent(Tool.class)) {
                    executors.put(method.getName(), new DefaultToolExecutor(toolObject, method));
                }
            }
        }
    }

    public List<ToolSpecification> getSpecifications() {
        return specifications;
    }

    public Map<String, ToolExecutor> getExecutors() {
        return executors;
    }
}
