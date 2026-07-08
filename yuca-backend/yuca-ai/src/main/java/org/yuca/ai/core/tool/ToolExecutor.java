package org.yuca.ai.core.tool;

/**
 * 工具执行器接口。
 * 等价于 langchain4j 的 dev.langchain4j.service.tool.ToolExecutor（去掉 memoryId 参数）。
 */
@FunctionalInterface
public interface ToolExecutor {

    /**
     * 执行工具调用。
     *
     * @param request 模型发起的工具调用请求，包含参数 JSON
     * @return 执行结果文本，会回传给模型
     */
    String execute(ToolExecutionRequest request);
}
