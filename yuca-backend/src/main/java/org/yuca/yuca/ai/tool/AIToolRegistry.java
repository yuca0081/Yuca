package org.yuca.yuca.ai.tool;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI 工具注册器
 *
 * <p>管理所有 AI 工具的注册、查询和执行
 *
 * @author Yuca
 * @since 2025-01-27
 */
@Slf4j
@Component
public class AIToolRegistry {

    /**
     * 工具存储（线程安全）
     */
    private final Map<String, IAITool> tools = new ConcurrentHashMap<>();

    /**
     * 注册工具
     *
     * @param tool 工具实例
     */
    public void register(IAITool tool) {
        tools.put(tool.getName(), tool);
        log.info("注册 AI 工具: {} - {}\n", tool.getName(), tool.getDescription());
    }

    /**
     * 获取所有已注册的工具
     *
     * @return 工具列表
     */
    public List<IAITool> getAllTools() {
        return new ArrayList<>(tools.values());
    }

    /**
     * 根据名称获取工具
     *
     * @param name 工具名称
     * @return 工具实例，如果不存在则返回 null
     */
    public IAITool getTool(String name) {
        return tools.get(name);
    }

    /**
     * 执行工具
     *
     * @param name 工具名称
     * @param args 参数
     * @return 执行结果
     * @throws RuntimeException 如果工具不存在或执行失败
     */
    public String executeTool(String name, JsonNode args) {
        IAITool tool = getTool(name);
        if (tool == null) {
            throw new RuntimeException("工具不存在: " + name);
        }
        return tool.execute(args);
    }
}
