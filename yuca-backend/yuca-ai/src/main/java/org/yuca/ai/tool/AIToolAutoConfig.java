package org.yuca.ai.tool;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * AI 工具自动配置
 *
 * <p>启动时自动扫描所有实现了 IAITool 接口的 Bean，并注册到 AIToolRegistry
 *
 * @author Yuca
 * @since 2025-01-27
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class AIToolAutoConfig {

    private final AIToolRegistry registry;
    private final ApplicationContext applicationContext;

    /**
     * 自动注册所有实现了 IAITool 接口的 Bean
     */
    @PostConstruct
    public void registerTools() {
        var tools = applicationContext.getBeansOfType(AITool.class);
        tools.values().forEach(registry::register);

        log.info("自动注册了 {} 个 AI 工具", tools.size());
    }
}
