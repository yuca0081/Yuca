package org.yuca.diet.tool;

import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.yuca.diet.dto.request.CreateRecordRequest;
import org.yuca.diet.dto.request.UpdateRecordRequest;
import org.yuca.diet.dto.response.DailySummaryResponse;
import org.yuca.diet.dto.response.DietGoalResponse;
import org.yuca.diet.dto.response.DietRecordResponse;
import org.yuca.diet.service.DietGoalService;
import org.yuca.diet.service.DietRecordService;
import org.yuca.infrastructure.security.SecurityUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 饮食工具
 * 暴露给 AI 模型的工具，模型通过这些工具读写用户的饮食数据
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DietTool {

    private final DietRecordService recordService;
    private final DietGoalService goalService;

    @Tool("记录一条饮食记录。recordDate为日期(YYYY-MM-DD)，mealType为餐次(1=早餐,2=午餐,3=晚餐,4=加餐)，foodName为食物名称，amount为食用量，unit为单位(如g/份/ml)，calories为热量(kcal)，protein为蛋白质(g)，fat为脂肪(g)，carbs为碳水(g)，remark为备注(可选)")
    public String addDietRecord(String recordDate, int mealType,
                                String foodName, double amount, String unit,
                                double calories, double protein, double fat, double carbs,
                                String remark) {
        long userId = SecurityUtils.getCurrentUserId();
        try {
            CreateRecordRequest request = new CreateRecordRequest();
            request.setRecordDate(LocalDate.parse(recordDate));
            request.setMealType(mealType);
            request.setFoodName(foodName);
            request.setAmount(BigDecimal.valueOf(amount));
            request.setUnit(unit != null ? unit : "g");
            request.setCalories(BigDecimal.valueOf(calories));
            request.setProtein(BigDecimal.valueOf(protein));
            request.setFat(BigDecimal.valueOf(fat));
            request.setCarbs(BigDecimal.valueOf(carbs));
            request.setRemark(remark);

            Long id = recordService.createRecord(request, userId);
            log.info("AI工具创建饮食记录成功: userId={}, recordId={}", userId, id);
            return "记录成功，ID: " + id;
        } catch (Exception e) {
            log.error("AI工具创建饮食记录失败: userId={}", userId, e);
            return "记录失败: " + e.getMessage();
        }
    }

    @Tool("修改一条饮食记录。id为记录ID（必填），其余参数均为可选，只传需要修改的字段：recordDate为日期(YYYY-MM-DD)，mealType为餐次(1=早餐,2=午餐,3=晚餐,4=加餐)，foodName为食物名称，amount为食用量，unit为单位，calories为热量(kcal)，protein为蛋白质(g)，fat为脂肪(g)，carbs为碳水(g)，remark为备注")
    public String updateDietRecord(long id,
                                   String recordDate, Integer mealType,
                                   String foodName, Double amount, String unit,
                                   Double calories, Double protein, Double fat, Double carbs,
                                   String remark) {
        long userId = SecurityUtils.getCurrentUserId();
        try {
            UpdateRecordRequest request = new UpdateRecordRequest();
            if (recordDate != null) request.setRecordDate(LocalDate.parse(recordDate));
            if (mealType != null) request.setMealType(mealType);
            if (foodName != null) request.setFoodName(foodName);
            if (amount != null) request.setAmount(BigDecimal.valueOf(amount));
            if (unit != null) request.setUnit(unit);
            if (calories != null) request.setCalories(BigDecimal.valueOf(calories));
            if (protein != null) request.setProtein(BigDecimal.valueOf(protein));
            if (fat != null) request.setFat(BigDecimal.valueOf(fat));
            if (carbs != null) request.setCarbs(BigDecimal.valueOf(carbs));
            if (remark != null) request.setRemark(remark);

            recordService.updateRecord(id, request, userId);
            log.info("AI工具更新饮食记录成功: userId={}, recordId={}", userId, id);
            return "修改成功，记录ID: " + id;
        } catch (Exception e) {
            log.error("AI工具更新饮食记录失败: userId={}, recordId={}", userId, id, e);
            return "修改失败: " + e.getMessage();
        }
    }

    @Tool("删除一条饮食记录。id为要删除的记录ID")
    public String deleteDietRecord(long id) {
        long userId = SecurityUtils.getCurrentUserId();
        try {
            recordService.deleteRecord(id, userId);
            log.info("AI工具删除饮食记录成功: userId={}, recordId={}", userId, id);
            return "删除成功，记录ID: " + id;
        } catch (Exception e) {
            log.error("AI工具删除饮食记录失败: userId={}, recordId={}", userId, id, e);
            return "删除失败: " + e.getMessage();
        }
    }

    @Tool("查询指定日期的饮食记录列表。date为日期(YYYY-MM-DD)")
    public String queryDietRecords(String date) {
        long userId = SecurityUtils.getCurrentUserId();
        try {
            List<DietRecordResponse> records = recordService.listByDate(LocalDate.parse(date), userId);
            if (records.isEmpty()) {
                return date + " 没有饮食记录";
            }
            return records.stream()
                    .map(r -> {
                        String base = String.format("[ID:%d] %s | %s | %s %s | 热量:%.0fkcal 蛋白:%.1fg 脂肪:%.1fg 碳水:%.1fg",
                                r.getId(),
                                r.getMealTypeLabel(),
                                r.getFoodName(),
                                r.getAmount().toPlainString(),
                                r.getUnit(),
                                r.getCalories() != null ? r.getCalories().doubleValue() : 0,
                                r.getProtein() != null ? r.getProtein().doubleValue() : 0,
                                r.getFat() != null ? r.getFat().doubleValue() : 0,
                                r.getCarbs() != null ? r.getCarbs().doubleValue() : 0);
                        if (r.getRemark() != null && !r.getRemark().isBlank()) {
                            base += " | 备注:" + r.getRemark();
                        }
                        return base;
                    })
                    .collect(Collectors.joining("\n"));
        } catch (Exception e) {
            log.error("AI工具查询饮食记录失败: userId={}", userId, e);
            return "查询失败: " + e.getMessage();
        }
    }

    @Tool("获取指定日期的营养汇总（含各餐次热量和宏量营养素总计）。date为日期(YYYY-MM-DD)")
    public String getDailyNutritionSummary(String date) {
        long userId = SecurityUtils.getCurrentUserId();
        try {
            DailySummaryResponse summary = recordService.getDailySummary(LocalDate.parse(date), userId);
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("日期: %s\n总热量: %.0f kcal | 蛋白质: %.1fg | 脂肪: %.1fg | 碳水: %.1fg\n",
                    date,
                    summary.getTotalCalories().doubleValue(),
                    summary.getTotalProtein().doubleValue(),
                    summary.getTotalFat().doubleValue(),
                    summary.getTotalCarbs().doubleValue()));

            if (summary.getMealSummaries() != null) {
                sb.append("各餐次:\n");
                for (DailySummaryResponse.MealSummary ms : summary.getMealSummaries()) {
                    sb.append(String.format("  %s: %.0fkcal (蛋白%.1fg 脂肪%.1fg 碳水%.1fg, %d条记录)\n",
                            ms.getMealTypeLabel(),
                            ms.getCalories().doubleValue(),
                            ms.getProtein().doubleValue(),
                            ms.getFat().doubleValue(),
                            ms.getCarbs().doubleValue(),
                            ms.getRecordCount()));
                }
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("AI工具获取营养汇总失败: userId={}", userId, e);
            return "获取汇总失败: " + e.getMessage();
        }
    }

    @Tool("获取用户的饮食目标")
    public String getDietGoal() {
        long userId = SecurityUtils.getCurrentUserId();
        try {
            DietGoalResponse goal = goalService.getGoal(userId);
            return String.format("每日目标: %d kcal | 蛋白质%.0f%% | 脂肪%.0f%% | 碳水%.0f%%",
                    goal.getDailyCalories(),
                    goal.getProteinRatio().doubleValue(),
                    goal.getFatRatio().doubleValue(),
                    goal.getCarbsRatio().doubleValue());
        } catch (Exception e) {
            log.error("AI工具获取饮食目标失败: userId={}", userId, e);
            return "获取目标失败: " + e.getMessage();
        }
    }
}
