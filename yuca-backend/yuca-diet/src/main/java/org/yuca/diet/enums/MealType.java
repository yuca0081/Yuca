package org.yuca.diet.enums;

import lombok.Getter;

/**
 * 餐次类型枚举
 */
@Getter
public enum MealType {

    BREAKFAST(1, "早餐"),
    LUNCH(2, "午餐"),
    DINNER(3, "晚餐"),
    SNACK(4, "加餐");

    private final int code;
    private final String description;

    MealType(int code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据代码获取枚举
     */
    public static MealType fromCode(int code) {
        for (MealType type : values()) {
            if (type.getCode() == code) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown meal type: " + code);
    }

    /**
     * 根据当前时间推荐餐次
     */
    public static MealType recommendByTime(int hour) {
        if (hour >= 6 && hour < 10) {
            return BREAKFAST;
        } else if (hour >= 11 && hour < 14) {
            return LUNCH;
        } else if (hour >= 17 && hour < 20) {
            return DINNER;
        } else {
            return SNACK;
        }
    }
}
