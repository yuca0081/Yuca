package org.yuca.ai.core.tool;

/**
 * 模型发起的工具调用请求。
 * 等价于 langchain4j 的 dev.langchain4j.agent.tool.ToolExecutionRequest。
 */
public record ToolExecutionRequest(String id, String name, String arguments) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String id;
        private String name;
        private String arguments;

        public Builder id(String id) { this.id = id; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder arguments(String arguments) { this.arguments = arguments; return this; }

        public ToolExecutionRequest build() {
            return new ToolExecutionRequest(id, name, arguments);
        }
    }
}
