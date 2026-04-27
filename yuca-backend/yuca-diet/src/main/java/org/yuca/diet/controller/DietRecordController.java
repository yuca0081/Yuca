package org.yuca.diet.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import org.yuca.common.annotation.RequireLogin;
import org.yuca.common.response.Result;
import org.yuca.diet.dto.request.CreateRecordRequest;
import org.yuca.diet.dto.request.UpdateRecordRequest;
import org.yuca.diet.dto.response.DailySummaryResponse;
import org.yuca.diet.dto.response.DietRecordResponse;
import org.yuca.diet.service.DietRecordService;
import org.yuca.infrastructure.security.SecurityUtils;

import java.time.LocalDate;
import java.util.List;

/**
 * 饮食记录Controller
 */
@Slf4j
@RestController
@RequestMapping("/diet/record")
@Tag(name = "饮食记录", description = "饮食记录的增删改查接口")
public class DietRecordController {

    @Autowired
    private DietRecordService dietRecordService;

    @PostMapping
    @Operation(summary = "新增饮食记录")
    @RequireLogin
    public Result<Long> createRecord(@Valid @RequestBody CreateRecordRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        Long id = dietRecordService.createRecord(request, userId);
        return Result.success(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改饮食记录")
    @RequireLogin
    public Result<Void> updateRecord(@PathVariable Long id,
                                      @Valid @RequestBody UpdateRecordRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        dietRecordService.updateRecord(id, request, userId);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除饮食记录")
    @RequireLogin
    public Result<Void> deleteRecord(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        dietRecordService.deleteRecord(id, userId);
        return Result.success();
    }

    @GetMapping("/list")
    @Operation(summary = "查询指定日期的记录列表")
    @RequireLogin
    public Result<List<DietRecordResponse>> listByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        Long userId = SecurityUtils.getCurrentUserId();
        List<DietRecordResponse> list = dietRecordService.listByDate(date, userId);
        return Result.success(list);
    }

    @GetMapping("/daily-summary")
    @Operation(summary = "查询指定日期的营养汇总")
    @RequireLogin
    public Result<DailySummaryResponse> getDailySummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        Long userId = SecurityUtils.getCurrentUserId();
        DailySummaryResponse summary = dietRecordService.getDailySummary(date, userId);
        return Result.success(summary);
    }

    @GetMapping("/recommend-meal")
    @Operation(summary = "获取推荐餐次")
    @RequireLogin
    public Result<Integer> getRecommendedMealType() {
        return Result.success(dietRecordService.getRecommendedMealType());
    }
}
