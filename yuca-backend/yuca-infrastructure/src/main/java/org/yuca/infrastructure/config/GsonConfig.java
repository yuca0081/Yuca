package org.yuca.infrastructure.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Gson 配置类
 */
@Configuration
public class GsonConfig {

    /**
     * 创建Gson Bean，用于JSON序列化
     */
    @Bean
    public Gson gson() {
        return new GsonBuilder()
                .setPrettyPrinting()  // 美化输出（可选）
                .serializeNulls()     // 序列化null字段
                .disableHtmlEscaping() // 禁用HTML转义
                .create();
    }
}
