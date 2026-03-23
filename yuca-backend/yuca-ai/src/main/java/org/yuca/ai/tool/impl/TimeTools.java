package org.yuca.ai.tool.impl;

import dev.langchain4j.service.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * 时间相关工具集
 * 使用 LangChain4j 的 @Tool 注解
 *
 * @author Yuca
 * @since 2025-01-27
 */
@Slf4j
@Component
public class TimeTools {

    /**
     * 获取当前时间
     *
     * @param format 时间格式，例如：yyyy-MM-dd HH:mm:ss（可选）
     * @return 格式化的当前时间字符串
     */
    @Tool("获取当前时间和日期，支持自定义格式")
    public String getCurrentTime(String format) {
        log.info("执行工具: getCurrentTime, format: {}", format);

        try {
            // 默认格式
            if (format == null || format.trim().isEmpty()) {
                format = "yyyy-MM-dd HH:mm:ss";
            }

            LocalDateTime now = LocalDateTime.now();
            String formattedTime = now.format(DateTimeFormatter.ofPattern(format));

            log.info("当前时间: {}", formattedTime);
            return "当前时间: " + formattedTime;
        } catch (Exception e) {
            log.error("getCurrentTime 执行失败", e);
            return "获取时间失败: " + e.getMessage();
        }
    }

    /**
     * 获取当前时间戳（毫秒）
     *
     * @return 当前时间戳
     */
    @Tool("获取当前时间戳（毫秒）")
    public long getCurrentTimestamp() {
        log.info("执行工具: getCurrentTimestamp");
        long timestamp = System.currentTimeMillis();
        log.info("当前时间戳: {}", timestamp);
        return timestamp;
    }

    /**
     * 获取当前日期（短格式）
     *
     * @return 当前日期字符串，格式：yyyy-MM-dd
     */
    @Tool("获取当前日期（年-月-日格式）")
    public String getCurrentDate() {
        log.info("执行工具: getCurrentDate");
        LocalDateTime now = LocalDateTime.now();
        String date = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        log.info("当前日期: {}", date);
        return date;
    }

    /**
     * 获取当前年份
     *
     * @return 当前年份
     */
    @Tool("获取当前年份")
    public int getCurrentYear() {
        log.info("执行工具: getCurrentYear");
        int year = LocalDateTime.now().getYear();
        log.info("当前年份: {}", year);
        return year;
    }

    /**
     * 获取当前月份
     *
     * @return 当前月份（1-12）
     */
    @Tool("获取当前月份（1-12）")
    public int getCurrentMonth() {
        log.info("执行工具: getCurrentMonth");
        int month = LocalDateTime.now().getMonthValue();
        log.info("当前月份: {}", month);
        return month;
    }

    /**
     * 获取当前日期（几号）
     *
     * @return 当前日期（1-31）
     */
    @Tool("获取当前日期（几号，1-31）")
    public int getCurrentDay() {
        log.info("执行工具: getCurrentDay");
        int day = LocalDateTime.now().getDayOfMonth();
        log.info("当前日期: {}", day);
        return day;
    }

    /**
     * 获取当前时间（时分秒格式）
     *
     * @return 当前时间字符串，格式：HH:mm:ss
     */
    @Tool("获取当前时间（时:分:秒格式）")
    public String getCurrentTimeOfDay() {
        log.info("执行工具: getCurrentTimeOfDay");
        LocalDateTime now = LocalDateTime.now();
        String time = now.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        log.info("当前时间: {}", time);
        return time;
    }
}
