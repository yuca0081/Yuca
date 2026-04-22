package org.yuca.ai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.yuca.ai.tool.ToolManager;
import org.yuca.ai.tool.Calculator;

/**
 * 工具配置
 * 在此注册所有可用工具
 */
@Configuration
public class ToolConfig {

    @Bean
    public ToolManager toolManager() {
        return new ToolManager()
                .register(new Calculator());
    }
}
