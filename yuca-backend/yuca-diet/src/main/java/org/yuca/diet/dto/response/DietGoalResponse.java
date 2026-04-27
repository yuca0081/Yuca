package org.yuca.diet.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 饮食目标响应DTO
 */
@Data
public class DietGoalResponse {

    private Integer dailyCalories;

    private BigDecimal proteinRatio;

    private BigDecimal fatRatio;

    private BigDecimal carbsRatio;

    private LocalDateTime updatedAt;
}
