package org.yuca.ai.core.provider.openai.dto;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * OpenAI 兼容格式的工具规格（type=function + function 描述）。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ToolSpecDto(
        String type,
        FunctionDef function
) {
    public static ToolSpecDto function(String name, String description, String parametersJson) {
        return new ToolSpecDto("function", new FunctionDef(name, description, parametersJson));
    }

    /**
     * 工具函数定义。parameters 字段是 JSON Schema 字符串，
     * 通过 {@link JsonRawValue} 让 Jackson 原样嵌入而不二次转义。
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FunctionDef(
            String name,
            String description,
            @JsonRawValue String parameters
    ) {}
}
