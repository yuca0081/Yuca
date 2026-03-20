package org.yuca.infrastructure.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * Jackson 配置
 * <p>
 * 配置 ObjectMapper 支持 Java 8 时间类型序列化
 *
 * @author Yuca
 * @since 2025-01-27
 */
@Configuration
public class JacksonConfig {

    /**
     * 配置 Jackson ObjectMapper（主要）
     * <p>
     * 使用 @Primary 确保优先使用此配置
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper objectMapper = builder.createXmlMapper(false).build();
        // 注册 Java 8 时间模块（支持 LocalDateTime、LocalDate 等）
        objectMapper.registerModule(new JavaTimeModule());
        // 禁用将日期写为时间戳（使用 ISO-8601 格式）
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // 忽略未知属性（提高容错性）
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return objectMapper;
    }
}
