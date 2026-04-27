package org.yuca.diet.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.yuca.common.exception.BusinessException;
import org.yuca.common.response.ErrorCode;
import org.yuca.diet.dto.request.UpdateGoalRequest;
import org.yuca.diet.dto.response.DietGoalResponse;
import org.yuca.diet.entity.DietGoal;
import org.yuca.diet.mapper.DietGoalMapper;

import java.math.BigDecimal;

/**
 * 用户饮食目标服务
 */
@Slf4j
@Service
public class DietGoalService extends ServiceImpl<DietGoalMapper, DietGoal> {

    /**
     * 获取用户目标（不存在则返回默认值）
     */
    public DietGoalResponse getGoal(Long userId) {
        DietGoal goal = getByUserId(userId);
        if (goal == null) {
            // 返回默认值，不持久化
            DietGoalResponse response = new DietGoalResponse();
            response.setDailyCalories(2000);
            response.setProteinRatio(BigDecimal.valueOf(20.00));
            response.setFatRatio(BigDecimal.valueOf(30.00));
            response.setCarbsRatio(BigDecimal.valueOf(50.00));
            return response;
        }
        return convertToResponse(goal);
    }

    /**
     * 获取目标实体（内部使用）
     */
    public DietGoal getGoalEntity(Long userId) {
        DietGoal goal = getByUserId(userId);
        if (goal == null) {
            // 返回默认实体
            return DietGoal.builder()
                    .userId(userId)
                    .dailyCalories(2000)
                    .proteinRatio(BigDecimal.valueOf(20.00))
                    .fatRatio(BigDecimal.valueOf(30.00))
                    .carbsRatio(BigDecimal.valueOf(50.00))
                    .build();
        }
        return goal;
    }

    /**
     * 设置/更新目标
     */
    @Transactional(rollbackFor = Exception.class)
    public DietGoalResponse updateGoal(UpdateGoalRequest request, Long userId) {
        // 校验比例之和为100
        BigDecimal sum = request.getProteinRatio()
                .add(request.getFatRatio())
                .add(request.getCarbsRatio());
        if (sum.compareTo(BigDecimal.valueOf(100)) != 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "蛋白质、脂肪、碳水占比之和必须为100%");
        }

        DietGoal existing = getByUserId(userId);
        if (existing == null) {
            // 新建
            DietGoal goal = DietGoal.builder()
                    .userId(userId)
                    .dailyCalories(request.getDailyCalories())
                    .proteinRatio(request.getProteinRatio())
                    .fatRatio(request.getFatRatio())
                    .carbsRatio(request.getCarbsRatio())
                    .build();
            this.baseMapper.insert(goal);
            log.info("创建饮食目标: userId={}", userId);
            return convertToResponse(goal);
        } else {
            // 更新
            existing.setDailyCalories(request.getDailyCalories());
            existing.setProteinRatio(request.getProteinRatio());
            existing.setFatRatio(request.getFatRatio());
            existing.setCarbsRatio(request.getCarbsRatio());
            this.baseMapper.updateById(existing);
            log.info("更新饮食目标: userId={}", userId);
            return convertToResponse(existing);
        }
    }

    /**
     * 根据用户ID查询目标
     */
    private DietGoal getByUserId(Long userId) {
        LambdaQueryWrapper<DietGoal> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DietGoal::getUserId, userId);
        return this.baseMapper.selectOne(wrapper);
    }

    /**
     * 转换为响应DTO
     */
    private DietGoalResponse convertToResponse(DietGoal goal) {
        DietGoalResponse response = new DietGoalResponse();
        response.setDailyCalories(goal.getDailyCalories());
        response.setProteinRatio(goal.getProteinRatio());
        response.setFatRatio(goal.getFatRatio());
        response.setCarbsRatio(goal.getCarbsRatio());
        response.setUpdatedAt(goal.getUpdatedAt());
        return response;
    }
}
