package org.yuca.ai.tool;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * AI 工具接口
 *
 * <p>所有可以被 AI 调用的工具都需要实现此接口
 * 实现类应该标注 @Component，以便自动注册到 AIToolRegistry
 *
 * @author Yuca
 * @since 2025-01-27
 */
public interface IAITool {

    /**
     * 获取工具名称（唯一标识）
     *
     * @return 工具名称，例如：get_current_weather
     */
    String getName();

    /**
     * 获取工具描述
     *
     * <p>描述会被发送给 AI，告诉 AI 这个工具的作用
     *
     * @return 工具描述，例如：获取指定地点的当前天气信息
     */
    String getDescription();

    /**
     * 获取参数定义（JSON Schema）
     *
     * @return 参数定义
     */
    AIToolParameters getParameters();

    /**
     * 执行工具
     *
     * @param args 参数（JSON 对象）
     * @return 执行结果（会返回给 AI）
     */
    String execute(JsonNode args);
}
