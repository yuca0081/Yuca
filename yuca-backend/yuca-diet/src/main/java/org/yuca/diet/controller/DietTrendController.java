package org.yuca.diet.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import org.yuca.common.annotation.RequireLogin;
import org.yuca.common.response.Result;
import org.yuca.diet.dto.response.TrendResponse;
import org.yuca.diet.service.DietRecordService;
import org.yuca.infrastructure.security.SecurityUtils;

import java.time.LocalDate;

/**
 * 饮食趋势统计Controller
 */
@Slf4j
@RestController
@RequestMapping("/diet/trend")
@Tag(name = "饮食趋势", description = "饮食数据的趋势统计接口")
public class DietTrendController {

    @Autowired
    private DietRecordService dietRecordService;

    @GetMapping("/weekly")
    @Operation(summary = "查询指定周的每日热量汇总")
    @RequireLogin
    public Result<TrendResponse> getWeeklyTrend(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        Long userId = SecurityUtils.getCurrentUserId();
        TrendResponse trend = dietRecordService.getWeeklyTrend(date, userId);
        return Result.success(trend);
    }

    @GetMapping("/monthly")
    @Operation(summary = "查询指定月的每日热量汇总")
    @RequireLogin
    public Result<TrendResponse> getMonthlyTrend(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        Long userId = SecurityUtils.getCurrentUserId();
        TrendResponse trend = dietRecordService.getMonthlyTrend(date, userId);
        return Result.success(trend);
    }
}
