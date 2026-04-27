package org.yuca.diet.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 更新饮食目标请求DTO
 */
@Data
public class UpdateGoalRequest {

    @NotNull(message = "每日热量目标不能为空")
    @Min(value = 500, message = "每日热量目标最少500kcal")
    @Max(value = 10000, message = "每日热量目标最多10000kcal")
    private Integer dailyCalories;

    @NotNull(message = "蛋白质占比不能为空")
    @DecimalMin(value = "0", message = "蛋白质占比不能为负")
    @DecimalMax(value = "100", message = "蛋白质占比不能超过100")
    private BigDecimal proteinRatio;

    @NotNull(message = "脂肪占比不能为空")
    @DecimalMin(value = "0", message = "脂肪占比不能为负")
    @DecimalMax(value = "100", message = "脂肪占比不能超过100")
    private BigDecimal fatRatio;

    @NotNull(message = "碳水占比不能为空")
    @DecimalMin(value = "0", message = "碳水占比不能为负")
    @DecimalMax(value = "100", message = "碳水占比不能超过100")
    private BigDecimal carbsRatio;
}
