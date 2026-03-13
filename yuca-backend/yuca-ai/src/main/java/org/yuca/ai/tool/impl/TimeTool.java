package org.yuca.ai.tool.impl;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.yuca.ai.tool.AIToolBase;
import org.yuca.ai.tool.AIToolParameters;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 时间查询工具
 *
 * @author Yuca
 * @since 2025-01-27
 */
@Slf4j
@Component
public class TimeTool extends AIToolBase {

    @Override
    public String getName() {
        return "get_current_time";
    }

    @Override
    public String getDescription() {
        return "获取当前时间和日期";
    }

    @Override
    public AIToolParameters getParameters() {
        return AIToolParameters.builder()
            .build()
            .addString("format", "时间格式，例如：yyyy-MM-dd HH:mm:ss（可选）");
    }

    @Override
    public String doExecute(JsonNode args) {
        log.info("执行工具: get_current_time");

        // 获取时间格式参数
        String format = "yyyy-MM-dd HH:mm:ss";
        if (args.has("format") && !args.get("format").isNull()) {
            format = args.get("format").asText(format);
        }

        // 获取当前时间
        LocalDateTime now = LocalDateTime.now();
        String formattedTime = now.format(DateTimeFormatter.ofPattern(format));

        log.info("当前时间: {}", formattedTime);
        return "当前时间: " + formattedTime;
    }
}
