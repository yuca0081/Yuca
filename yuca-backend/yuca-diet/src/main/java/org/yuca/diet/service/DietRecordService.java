package org.yuca.diet.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.yuca.common.exception.BusinessException;
import org.yuca.common.response.ErrorCode;
import org.yuca.diet.dto.request.CreateRecordRequest;
import org.yuca.diet.dto.request.UpdateRecordRequest;
import org.yuca.diet.dto.response.DailySummaryResponse;
import org.yuca.diet.dto.response.DietRecordResponse;
import org.yuca.diet.dto.response.TrendResponse;
import org.yuca.diet.entity.DietRecord;
import org.yuca.diet.enums.MealType;
import org.yuca.diet.mapper.DietRecordMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 饮食记录服务
 */
@Slf4j
@Service
public class DietRecordService extends ServiceImpl<DietRecordMapper, DietRecord> {

    @Autowired
    private DietGoalService dietGoalService;

    /**
     * 创建饮食记录
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createRecord(CreateRecordRequest request, Long userId) {
        DietRecord record = DietRecord.builder()
                .userId(userId)
                .recordDate(request.getRecordDate())
                .mealType(request.getMealType())
                .foodName(request.getFoodName())
                .amount(request.getAmount())
                .unit(request.getUnit() != null ? request.getUnit() : "g")
                .calories(request.getCalories())
                .protein(request.getProtein())
                .fat(request.getFat())
                .carbs(request.getCarbs())
                .remark(request.getRemark())
                .build();

        this.baseMapper.insert(record);
        log.info("创建饮食记录成功: userId={}, recordId={}", userId, record.getId());
        return record.getId();
    }

    /**
     * 更新饮食记录
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateRecord(Long id, UpdateRecordRequest request, Long userId) {
        DietRecord record = getAndValidateOwnership(id, userId);

        if (request.getRecordDate() != null) {
            record.setRecordDate(request.getRecordDate());
        }
        if (request.getMealType() != null) {
            record.setMealType(request.getMealType());
        }
        if (request.getFoodName() != null) {
            record.setFoodName(request.getFoodName());
        }
        if (request.getAmount() != null) {
            record.setAmount(request.getAmount());
        }
        if (request.getUnit() != null) {
            record.setUnit(request.getUnit());
        }
        if (request.getCalories() != null) {
            record.setCalories(request.getCalories());
        }
        if (request.getProtein() != null) {
            record.setProtein(request.getProtein());
        }
        if (request.getFat() != null) {
            record.setFat(request.getFat());
        }
        if (request.getCarbs() != null) {
            record.setCarbs(request.getCarbs());
        }
        if (request.getRemark() != null) {
            record.setRemark(request.getRemark());
        }

        this.baseMapper.updateById(record);
        log.info("更新饮食记录成功: recordId={}", id);
    }

    /**
     * 删除饮食记录
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteRecord(Long id, Long userId) {
        getAndValidateOwnership(id, userId);
        this.baseMapper.deleteById(id);
        log.info("删除饮食记录成功: recordId={}", id);
    }

    /**
     * 查询指定日期的记录列表
     */
    public List<DietRecordResponse> listByDate(LocalDate date, Long userId) {
        LambdaQueryWrapper<DietRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DietRecord::getUserId, userId)
                .eq(DietRecord::getRecordDate, date)
                .orderByAsc(DietRecord::getMealType)
                .orderByDesc(DietRecord::getCreatedAt);

        List<DietRecord> records = this.baseMapper.selectList(wrapper);
        return records.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 获取每日营养汇总
     */
    public DailySummaryResponse getDailySummary(LocalDate date, Long userId) {
        LambdaQueryWrapper<DietRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DietRecord::getUserId, userId)
                .eq(DietRecord::getRecordDate, date);

        List<DietRecord> records = this.baseMapper.selectList(wrapper);

        DailySummaryResponse summary = new DailySummaryResponse();
        summary.setDate(date);

        if (records.isEmpty()) {
            summary.setTotalCalories(BigDecimal.ZERO);
            summary.setTotalProtein(BigDecimal.ZERO);
            summary.setTotalFat(BigDecimal.ZERO);
            summary.setTotalCarbs(BigDecimal.ZERO);
            summary.setMealSummaries(Collections.emptyList());
            return summary;
        }

        // 按餐次分组
        Map<Integer, List<DietRecord>> byMealType = records.stream()
                .collect(Collectors.groupingBy(DietRecord::getMealType, TreeMap::new, Collectors.toList()));

        List<DailySummaryResponse.MealSummary> mealSummaries = new ArrayList<>();
        BigDecimal totalCalories = BigDecimal.ZERO;
        BigDecimal totalProtein = BigDecimal.ZERO;
        BigDecimal totalFat = BigDecimal.ZERO;
        BigDecimal totalCarbs = BigDecimal.ZERO;

        for (Map.Entry<Integer, List<DietRecord>> entry : byMealType.entrySet()) {
            DailySummaryResponse.MealSummary ms = new DailySummaryResponse.MealSummary();
            ms.setMealType(entry.getKey());
            ms.setMealTypeLabel(MealType.fromCode(entry.getKey()).getDescription());
            ms.setRecordCount(entry.getValue().size());

            BigDecimal mealCal = entry.getValue().stream()
                    .map(DietRecord::getCalories).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal mealPro = entry.getValue().stream()
                    .map(r -> r.getProtein() != null ? r.getProtein() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal mealFat = entry.getValue().stream()
                    .map(r -> r.getFat() != null ? r.getFat() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal mealCarbs = entry.getValue().stream()
                    .map(r -> r.getCarbs() != null ? r.getCarbs() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);

            ms.setCalories(mealCal);
            ms.setProtein(mealPro);
            ms.setFat(mealFat);
            ms.setCarbs(mealCarbs);

            mealSummaries.add(ms);

            totalCalories = totalCalories.add(mealCal);
            totalProtein = totalProtein.add(mealPro);
            totalFat = totalFat.add(mealFat);
            totalCarbs = totalCarbs.add(mealCarbs);
        }

        summary.setTotalCalories(totalCalories);
        summary.setTotalProtein(totalProtein);
        summary.setTotalFat(totalFat);
        summary.setTotalCarbs(totalCarbs);
        summary.setMealSummaries(mealSummaries);

        return summary;
    }

    /**
     * 获取推荐餐次
     */
    public int getRecommendedMealType() {
        return MealType.recommendByTime(LocalDateTime.now().getHour()).getCode();
    }

    /**
     * 获取周趋势数据
     */
    public TrendResponse getWeeklyTrend(LocalDate weekStart, Long userId) {
        LocalDate start = weekStart != null ? weekStart : LocalDate.now().minusDays(LocalDate.now().getDayOfWeek().getValue() - 1);
        LocalDate end = start.plusDays(6);
        return buildTrendResponse(start, end, userId, false);
    }

    /**
     * 获取月趋势数据
     */
    public TrendResponse getMonthlyTrend(LocalDate monthStart, Long userId) {
        LocalDate start = monthStart != null ? monthStart : LocalDate.now().withDayOfMonth(1);
        LocalDate end = start.plusMonths(1).minusDays(1);
        return buildTrendResponse(start, end, userId, true);
    }

    /**
     * 构建趋势响应
     */
    private TrendResponse buildTrendResponse(LocalDate start, LocalDate end, Long userId, boolean includeTargetDays) {
        LambdaQueryWrapper<DietRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DietRecord::getUserId, userId)
                .between(DietRecord::getRecordDate, start, end);

        List<DietRecord> records = this.baseMapper.selectList(wrapper);

        // 按日期分组
        Map<LocalDate, List<DietRecord>> byDate = records.stream()
                .collect(Collectors.groupingBy(DietRecord::getRecordDate, TreeMap::new, Collectors.toList()));

        List<TrendResponse.DailyTrendItem> items = new ArrayList<>();
        BigDecimal maxCalories = BigDecimal.ZERO;
        BigDecimal minCalories = null;
        BigDecimal totalCalories = BigDecimal.ZERO;

        // 目标热量
        BigDecimal targetCalories = null;
        if (includeTargetDays) {
            Integer goal = dietGoalService.getGoalEntity(userId).getDailyCalories();
            targetCalories = BigDecimal.valueOf(goal);
        }

        int targetDaysCount = 0;

        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            TrendResponse.DailyTrendItem item = new TrendResponse.DailyTrendItem();
            item.setDate(date);

            List<DietRecord> dayRecords = byDate.getOrDefault(date, Collections.emptyList());
            if (dayRecords.isEmpty()) {
                item.setCalories(BigDecimal.ZERO);
                item.setProtein(BigDecimal.ZERO);
                item.setFat(BigDecimal.ZERO);
                item.setCarbs(BigDecimal.ZERO);
            } else {
                BigDecimal cal = dayRecords.stream().map(DietRecord::getCalories).reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal pro = dayRecords.stream().map(r -> r.getProtein() != null ? r.getProtein() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal fat = dayRecords.stream().map(r -> r.getFat() != null ? r.getFat() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal carbs = dayRecords.stream().map(r -> r.getCarbs() != null ? r.getCarbs() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);

                item.setCalories(cal);
                item.setProtein(pro);
                item.setFat(fat);
                item.setCarbs(carbs);

                totalCalories = totalCalories.add(cal);
                if (cal.compareTo(maxCalories) > 0) {
                    maxCalories = cal;
                }
                if (minCalories == null || cal.compareTo(minCalories) < 0) {
                    minCalories = cal;
                }

                // 判断是否达标
                if (targetCalories != null && !dayRecords.isEmpty()) {
                    BigDecimal lower = targetCalories.multiply(BigDecimal.valueOf(0.9));
                    BigDecimal upper = targetCalories.multiply(BigDecimal.valueOf(1.1));
                    if (cal.compareTo(lower) >= 0 && cal.compareTo(upper) <= 0) {
                        targetDaysCount++;
                    }
                }
            }

            items.add(item);
        }

        TrendResponse response = new TrendResponse();
        response.setItems(items);
        response.setMaxCalories(maxCalories);
        response.setMinCalories(minCalories != null ? minCalories : BigDecimal.ZERO);

        long daysWithRecords = byDate.values().stream().filter(list -> !list.isEmpty()).count();
        if (daysWithRecords > 0) {
            response.setAverageCalories(totalCalories.divide(BigDecimal.valueOf(daysWithRecords), 2, RoundingMode.HALF_UP));
        } else {
            response.setAverageCalories(BigDecimal.ZERO);
        }

        if (includeTargetDays) {
            response.setTargetDays(targetDaysCount);
        }

        return response;
    }

    /**
     * 获取记录并验证所属权
     */
    private DietRecord getAndValidateOwnership(Long id, Long userId) {
        DietRecord record = this.baseMapper.selectById(id);
        if (record == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "记录不存在");
        }
        if (!record.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作此记录");
        }
        return record;
    }

    /**
     * 转换为响应DTO
     */
    private DietRecordResponse convertToResponse(DietRecord record) {
        DietRecordResponse response = new DietRecordResponse();
        response.setId(record.getId());
        response.setRecordDate(record.getRecordDate());
        response.setMealType(record.getMealType());
        response.setMealTypeLabel(MealType.fromCode(record.getMealType()).getDescription());
        response.setFoodName(record.getFoodName());
        response.setAmount(record.getAmount());
        response.setUnit(record.getUnit());
        response.setCalories(record.getCalories());
        response.setProtein(record.getProtein());
        response.setFat(record.getFat());
        response.setCarbs(record.getCarbs());
        response.setRemark(record.getRemark());
        response.setCreatedAt(record.getCreatedAt());
        response.setUpdatedAt(record.getUpdatedAt());
        return response;
    }
}
