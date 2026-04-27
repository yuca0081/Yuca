package org.yuca.diet.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 饮食记录实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("diet_record")
public class DietRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属用户ID
     */
    private Long userId;

    /**
     * 记录日期
     */
    private LocalDate recordDate;

    /**
     * 餐次：1=早餐 2=午餐 3=晚餐 4=加餐
     */
    private Integer mealType;

    /**
     * 食物名称
     */
    private String foodName;

    /**
     * 食用量
     */
    private BigDecimal amount;

    /**
     * 单位（g/份/ml）
     */
    private String unit;

    /**
     * 热量（kcal）
     */
    private BigDecimal calories;

    /**
     * 蛋白质（g）
     */
    private BigDecimal protein;

    /**
     * 脂肪（g）
     */
    private BigDecimal fat;

    /**
     * 碳水（g）
     */
    private BigDecimal carbs;

    /**
     * 备注
     */
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
