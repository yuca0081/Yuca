package org.yuca.diet.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 更新饮食记录请求DTO（所有字段可选）
 */
@Data
public class UpdateRecordRequest {

    private LocalDate recordDate;

    @Min(value = 1, message = "餐次值无效")
    @Max(value = 4, message = "餐次值无效")
    private Integer mealType;

    @Size(max = 100, message = "食物名称最多100个字符")
    private String foodName;

    @DecimalMin(value = "0.01", message = "食用量必须大于0")
    private BigDecimal amount;

    @Size(max = 10, message = "单位最多10个字符")
    private String unit;

    @DecimalMin(value = "0", message = "热量不能为负")
    private BigDecimal calories;

    @DecimalMin(value = "0", message = "蛋白质不能为负")
    private BigDecimal protein;

    @DecimalMin(value = "0", message = "脂肪不能为负")
    private BigDecimal fat;

    @DecimalMin(value = "0", message = "碳水不能为负")
    private BigDecimal carbs;

    @Size(max = 255, message = "备注最多255个字符")
    private String remark;
}
