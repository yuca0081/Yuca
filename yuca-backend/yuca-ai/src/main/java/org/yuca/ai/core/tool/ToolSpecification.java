package org.yuca.ai.core.tool;

/**
 * 工具规格，描述一个可被 LLM 调用的工具。
 * <p>
 * parameters 字段是 JSON Schema 字符串（draft-07 风格），描述方法参数结构。
 * 等价于 langchain4j 的 dev.langchain4j.agent.tool.ToolSpecification。
 */
public record ToolSpecification(String name, String description, String parameters) {

    public static ToolSpecification of(String name, String description, String parameters) {
        return new ToolSpecification(name, description, parameters);
    }
}
