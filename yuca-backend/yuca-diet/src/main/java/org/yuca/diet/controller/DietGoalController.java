package org.yuca.diet.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.yuca.common.annotation.RequireLogin;
import org.yuca.common.response.Result;
import org.yuca.diet.dto.request.UpdateGoalRequest;
import org.yuca.diet.dto.response.DietGoalResponse;
import org.yuca.diet.service.DietGoalService;
import org.yuca.infrastructure.security.SecurityUtils;

/**
 * 饮食目标Controller
 */
@Slf4j
@RestController
@RequestMapping("/diet/goal")
@Tag(name = "饮食目标", description = "饮食目标的设置与查询接口")
public class DietGoalController {

    @Autowired
    private DietGoalService dietGoalService;

    @GetMapping
    @Operation(summary = "获取当前用户目标")
    @RequireLogin
    public Result<DietGoalResponse> getGoal() {
        Long userId = SecurityUtils.getCurrentUserId();
        DietGoalResponse goal = dietGoalService.getGoal(userId);
        return Result.success(goal);
    }

    @PutMapping
    @Operation(summary = "设置/更新目标")
    @RequireLogin
    public Result<DietGoalResponse> updateGoal(@Valid @RequestBody UpdateGoalRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        DietGoalResponse goal = dietGoalService.updateGoal(request, userId);
        return Result.success(goal);
    }
}
