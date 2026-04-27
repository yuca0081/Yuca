package org.yuca.diet.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 饮食记录响应DTO
 */
@Data
public class DietRecordResponse {

    private Long id;

    private LocalDate recordDate;

    private Integer mealType;

    private String mealTypeLabel;

    private String foodName;

    private BigDecimal amount;

    private String unit;

    private BigDecimal calories;

    private BigDecimal protein;

    private BigDecimal fat;

    private BigDecimal carbs;

    private String remark;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
