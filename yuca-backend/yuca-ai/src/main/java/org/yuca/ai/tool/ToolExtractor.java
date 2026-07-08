package org.yuca.ai.tool;

import org.yuca.ai.core.tool.ReflectiveToolExecutor;
import org.yuca.ai.core.tool.Tool;
import org.yuca.ai.core.tool.ToolExecutor;
import org.yuca.ai.core.tool.ToolSchemaGenerator;
import org.yuca.ai.core.tool.ToolSpecification;

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
        ToolSchemaGenerator generator = new ToolSchemaGenerator();
        for (Object toolObject : toolObjects) {
            specifications.addAll(generator.generate(toolObject));
            for (Method method : toolObject.getClass().getDeclaredMethods()) {
                if (method.isAnnotationPresent(Tool.class)) {
                    executors.put(method.getName(), new ReflectiveToolExecutor(toolObject, method));
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
