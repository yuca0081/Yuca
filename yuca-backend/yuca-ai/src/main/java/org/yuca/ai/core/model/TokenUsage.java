package org.yuca.ai.core.model;

/**
 * Token 使用统计。
 * 等价于 langchain4j 的 dev.langchain4j.model.output.TokenUsage。
 */
public record TokenUsage(Integer inputTokenCount, Integer outputTokenCount, Integer totalTokenCount) {

    public static TokenUsage of(Integer input, Integer output, Integer total) {
        return new TokenUsage(input, output, total);
    }
}
