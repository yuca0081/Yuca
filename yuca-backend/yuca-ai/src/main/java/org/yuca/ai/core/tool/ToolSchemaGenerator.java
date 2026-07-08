package org.yuca.ai.core.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 工具 Schema 生成器。
 * <p>
 * 扫描对象类上带 {@link Tool} 注解的方法，按参数类型生成 JSON Schema，
 * 等价替换 langchain4j 的 {@code ToolSpecifications.toolSpecificationsFrom(Object)}。
 * <p>
 * 支持的参数类型：String、int/long/short/byte、float/double、boolean、
 * BigDecimal/BigInteger、LocalDate/LocalDateTime、UUID。
 * 复杂 POJO 暂不支持（项目当前未用到，遇到时按需扩展）。
 */
@Slf4j
public class ToolSchemaGenerator {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * 扫描对象上所有带 @Tool 的方法，生成对应的工具规格。
     */
    public List<ToolSpecification> generate(Object toolObject) {
        List<ToolSpecification> specs = new ArrayList<>();
        for (Method method : toolObject.getClass().getDeclaredMethods()) {
            Tool annotation = method.getAnnotation(Tool.class);
            if (annotation == null) {
                continue;
            }
            specs.add(buildSpecification(method, annotation));
        }
        return specs;
    }

    /**
     * 扫描多个工具对象。
     */
    public List<ToolSpecification> generateAll(List<Object> toolObjects) {
        List<ToolSpecification> all = new ArrayList<>();
        for (Object obj : toolObjects) {
            all.addAll(generate(obj));
        }
        return all;
    }

    private ToolSpecification buildSpecification(Method method, Tool annotation) {
        String name = method.getName();
        String description = annotation.value();
        String parameters = buildParametersJson(method);
        return new ToolSpecification(name, description, parameters);
    }

    private String buildParametersJson(Method method) {
        Parameter[] params = method.getParameters();
        if (params.length == 0) {
            return null;
        }

        ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        ArrayNode required = schema.putArray("required");

        for (Parameter param : params) {
            String paramName = param.getName();
            // 反射拿不到参数名（编译时未启用 -parameters）时回退
            if (paramName == null || paramName.startsWith("arg")) {
                log.warn("无法反射获取参数名（请启用 -parameters 编译选项），方法: {}.{}",
                        method.getDeclaringClass().getSimpleName(), method.getName());
            }
            ObjectNode propSchema = jsonSchemaFor(param.getType());
            if (propSchema != null) {
                properties.set(paramName, propSchema);
                required.add(paramName);
            }
        }

        try {
            return MAPPER.writeValueAsString(schema);
        } catch (Exception e) {
            throw new IllegalStateException("序列化工具参数 schema 失败: " + method, e);
        }
    }

    private ObjectNode jsonSchemaFor(Class<?> type) {
        ObjectNode node = MAPPER.createObjectNode();
        if (type == String.class || type == char.class || type == Character.class
                || type == UUID.class) {
            node.put("type", "string");
        } else if (type == boolean.class || type == Boolean.class) {
            node.put("type", "boolean");
        } else if (type == int.class || type == Integer.class
                || type == long.class || type == Long.class
                || type == short.class || type == Short.class
                || type == byte.class || type == Byte.class
                || type == BigInteger.class) {
            node.put("type", "integer");
        } else if (type == double.class || type == Double.class
                || type == float.class || type == Float.class
                || type == BigDecimal.class) {
            node.put("type", "number");
        } else if (type == LocalDate.class) {
            node.put("type", "string");
            node.put("format", "date");
        } else if (type == LocalDateTime.class) {
            node.put("type", "string");
            node.put("format", "date-time");
        } else if (type.isEnum()) {
            node.put("type", "string");
            ArrayNode enumValues = node.putArray("enum");
            for (Object constant : type.getEnumConstants()) {
                enumValues.add(((Enum<?>) constant).name());
            }
        } else {
            throw new IllegalArgumentException("不支持的工具参数类型: " + type.getName()
                    + "（如需支持复杂 POJO，请扩展 ToolSchemaGenerator）");
        }
        return node;
    }
}
