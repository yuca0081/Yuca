package org.yuca.ai.tool.impl;

import dev.langchain4j.service.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 计算器工具集
 * 使用 LangChain4j 的 @Tool 注解
 *
 * @author Yuca
 * @since 2025-01-27
 */
@Slf4j
@Component
public class CalculatorTools {

    /**
     * 计算两个数的和
     *
     * @param a 第一个数
     * @param b 第二个数
     * @return 和
     */
    @Tool("计算两个数的和")
    public double add(double a, double b) {
        log.info("执行工具: add, a: {}, b: {}", a, b);
        double result = a + b;
        log.info("计算结果: {}", result);
        return result;
    }

    /**
     * 计算两个数的差
     *
     * @param a 被减数
     * @param b 减数
     * @return 差
     */
    @Tool("计算两个数的差（第一个数减去第二个数）")
    public double subtract(double a, double b) {
        log.info("执行工具: subtract, a: {}, b: {}", a, b);
        double result = a - b;
        log.info("计算结果: {}", result);
        return result;
    }

    /**
     * 计算两个数的乘积
     *
     * @param a 第一个数
     * @param b 第二个数
     * @return 乘积
     */
    @Tool("计算两个数的乘积")
    public double multiply(double a, double b) {
        log.info("执行工具: multiply, a: {}, b: {}", a, b);
        double result = a * b;
        log.info("计算结果: {}", result);
        return result;
    }

    /**
     * 计算两个数的商
     *
     * @param a 被除数
     * @param b 除数
     * @return 商
     */
    @Tool("计算两个数的商（第一个数除以第二个数）")
    public double divide(double a, double b) {
        log.info("执行工具: divide, a: {}, b: {}", a, b);
        if (b == 0) {
            log.error("除数不能为零");
            throw new IllegalArgumentException("除数不能为零");
        }
        double result = a / b;
        log.info("计算结果: {}", result);
        return result;
    }

    /**
     * 计算幂
     *
     * @param base     底数
     * @param exponent 指数
     * @return 幂结果
     */
    @Tool("计算一个数的幂（底数的指数次方）")
    public double power(double base, double exponent) {
        log.info("执行工具: power, base: {}, exponent: {}", base, exponent);
        double result = Math.pow(base, exponent);
        log.info("计算结果: {}", result);
        return result;
    }

    /**
     * 计算平方根
     *
     * @param number 数值
     * @return 平方根
     */
    @Tool("计算一个数的平方根")
    public double squareRoot(double number) {
        log.info("执行工具: squareRoot, number: {}", number);
        if (number < 0) {
            log.error("不能计算负数的平方根");
            throw new IllegalArgumentException("不能计算负数的平方根");
        }
        double result = Math.sqrt(number);
        log.info("计算结果: {}", result);
        return result;
    }

    /**
     * 计算绝对值
     *
     * @param number 数值
     * @return 绝对值
     */
    @Tool("计算一个数的绝对值")
    public double absoluteValue(double number) {
        log.info("执行工具: absoluteValue, number: {}", number);
        double result = Math.abs(number);
        log.info("计算结果: {}", result);
        return result;
    }

    /**
     * 四舍五入
     *
     * @param number 数值
     * @param digits 保留小数位数
     * @return 四舍五入后的数值
     */
    @Tool("对数值进行四舍五入，可指定保留小数位数")
    public double round(double number, int digits) {
        log.info("执行工具: round, number: {}, digits: {}", number, digits);
        double scale = Math.pow(10, digits);
        double result = Math.round(number * scale) / scale;
        log.info("计算结果: {}", result);
        return result;
    }
}
