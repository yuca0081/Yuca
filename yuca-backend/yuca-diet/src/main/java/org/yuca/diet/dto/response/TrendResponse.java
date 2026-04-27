package org.yuca.diet.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 趋势统计响应DTO
 */
@Data
public class TrendResponse {

    private List<DailyTrendItem> items;

    private BigDecimal averageCalories;

    private BigDecimal maxCalories;

    private BigDecimal minCalories;

    /**
     * 达标天数（月度统计时使用）
     */
    private Integer targetDays;

    /**
     * 每日趋势数据
     */
    @Data
    public static class DailyTrendItem {

        private LocalDate date;

        private BigDecimal calories;

        private BigDecimal protein;

        private BigDecimal fat;

        private BigDecimal carbs;
    }
}
