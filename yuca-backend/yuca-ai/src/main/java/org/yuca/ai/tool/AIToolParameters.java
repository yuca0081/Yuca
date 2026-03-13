package org.yuca.ai.tool;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 工具参数定义
 *
 * <p>用于定义工具的参数结构（简化版 JSON Schema）
 *
 * @author Yuca
 * @since 2025-01-27
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIToolParameters {

    /**
     * 类型（固定为 object）
     */
    @Builder.Default
    private String type = "object";

    /**
     * 参数属性定义
     */
    private Map<String, ParameterProperty> properties;

    /**
     * 必填参数列表
     */
    @Builder.Default
    private List<String> required = new ArrayList<>();

    /**
     * 添加字符串参数
     *
     * @param name 参数名称
     * @param description 参数描述
     * @return this
     */
    public AIToolParameters addString(String name, String description) {
        return addProperty(name, "string", description);
    }

    /**
     * 添加整数参数
     *
     * @param name 参数名称
     * @param description 参数描述
     * @return this
     */
    public AIToolParameters addInteger(String name, String description) {
        return addProperty(name, "integer", description);
    }

    /**
     * 添加布尔参数
     *
     * @param name 参数名称
     * @param description 参数描述
     * @return this
     */
    public AIToolParameters addBoolean(String name, String description) {
        return addProperty(name, "boolean", description);
    }

    /**
     * 添加必填参数
     *
     * @param name 参数名称
     * @return this
     */
    public AIToolParameters addRequired(String name) {
        if (required == null) {
            required = new ArrayList<>();
        }
        required.add(name);
        return this;
    }

    /**
     * 添加属性
     *
     * @param name 参数名称
     * @param type 参数类型
     * @param description 参数描述
     * @return this
     */
    private AIToolParameters addProperty(String name, String type, String description) {
        if (properties == null) {
            properties = new LinkedHashMap<>();
        }
        properties.put(name, ParameterProperty.builder()
            .type(type)
            .description(description)
            .build());
        return this;
    }

    /**
     * 参数属性定义
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParameterProperty {
        /**
         * 参数类型
         */
        private String type;

        /**
         * 参数描述
         */
        private String description;
    }
}
