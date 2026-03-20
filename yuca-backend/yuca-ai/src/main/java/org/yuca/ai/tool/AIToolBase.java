package org.yuca.ai.tool;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;

/**
 * AI 工具基类
 *
 * <p>提供工具执行的默认实现，简化子类开发
 * 子类只需实现 doExecute 方法，可以注入 Spring Bean
 *
 * @author Yuca
 * @since 2025-01-27
 */
@Slf4j
public abstract class AIToolBase implements AITool {

    /**
     * 子类实现此方法，可以注入任意 Spring Bean
     *
     * @param args 参数
     * @return 执行结果
     */
    public abstract String doExecute(JsonNode args);

    @Override
    public String execute(JsonNode args) {
        try {
            return doExecute(args);
        } catch (Exception e) {
            log.error("工具执行失败: {}", getName(), e);
            throw new RuntimeException("工具执行失败: " + e.getMessage(), e);
        }
    }
}
