package org.yuca.diet.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 每日营养汇总响应DTO
 */
@Data
public class DailySummaryResponse {

    private LocalDate date;

    private BigDecimal totalCalories;

    private BigDecimal totalProtein;

    private BigDecimal totalFat;

    private BigDecimal totalCarbs;

    private List<MealSummary> mealSummaries;

    /**
     * 餐次汇总
     */
    @Data
    public static class MealSummary {

        private int mealType;

        private String mealTypeLabel;

        private BigDecimal calories;

        private BigDecimal protein;

        private BigDecimal fat;

        private BigDecimal carbs;

        private int recordCount;
    }
}
