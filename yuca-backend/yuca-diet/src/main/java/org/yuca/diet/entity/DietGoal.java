package org.yuca.diet.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户饮食目标实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("diet_goal")
public class DietGoal {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID（唯一）
     */
    private Long userId;

    /**
     * 每日热量目标（kcal）
     */
    private Integer dailyCalories;

    /**
     * 蛋白质目标占比（%）
     */
    private BigDecimal proteinRatio;

    /**
     * 脂肪目标占比（%）
     */
    private BigDecimal fatRatio;

    /**
     * 碳水目标占比（%）
     */
    private BigDecimal carbsRatio;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
