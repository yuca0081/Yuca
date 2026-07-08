package org.yuca.ai.core.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 反射式工具执行器：持有目标对象 + 方法，将模型传入的 JSON 参数反序列化后反射调用。
 * 等价替换 langchain4j 的 {@code dev.langchain4j.service.tool.DefaultToolExecutor}。
 */
@Slf4j
public class ReflectiveToolExecutor implements ToolExecutor {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Object target;
    private final Method method;

    public ReflectiveToolExecutor(Object target, Method method) {
        this.target = target;
        this.method = method;
        if (!method.canAccess(target)) {
            method.setAccessible(true);
        }
    }

    @Override
    public String execute(ToolExecutionRequest request) {
        try {
            Object[] args = bindArguments(request.arguments());
            Object result = method.invoke(target, args);
            return result == null ? "" : result.toString();
        } catch (Exception e) {
            log.error("工具反射执行失败: {} args={}", request.name(), request.arguments(), e);
            throw new RuntimeException("工具执行失败: " + e.getMessage(), e);
        }
    }

    private Object[] bindArguments(String argumentsJson) throws Exception {
        Parameter[] params = method.getParameters();
        if (params.length == 0) {
            return new Object[0];
        }
        JsonNode root = (argumentsJson == null || argumentsJson.isBlank())
                ? MAPPER.createObjectNode()
                : MAPPER.readTree(argumentsJson);

        Object[] args = new Object[params.length];
        for (int i = 0; i < params.length; i++) {
            Parameter param = params[i];
            JsonNode valueNode = root.get(param.getName());
            args[i] = convert(valueNode, param.getType());
        }
        return args;
    }

    private Object convert(JsonNode node, Class<?> targetType) {
        if (node == null || node.isNull()) {
            return defaultValueFor(targetType);
        }
        if (targetType == String.class) {
            return node.asText();
        }
        if (targetType == boolean.class || targetType == Boolean.class) {
            return node.asBoolean();
        }
        if (targetType == int.class || targetType == Integer.class) {
            return node.asInt();
        }
        if (targetType == long.class || targetType == Long.class) {
            return node.asLong();
        }
        if (targetType == short.class || targetType == Short.class) {
            return (short) node.asInt();
        }
        if (targetType == byte.class || targetType == Byte.class) {
            return (byte) node.asInt();
        }
        if (targetType == double.class || targetType == Double.class) {
            return node.asDouble();
        }
        if (targetType == float.class || targetType == Float.class) {
            return (float) node.asDouble();
        }
        if (targetType == BigDecimal.class) {
            return node.isNumber() ? new BigDecimal(node.asText()) : BigDecimal.ZERO;
        }
        if (targetType == BigInteger.class) {
            return node.isNumber() ? new BigInteger(node.asText()) : BigInteger.ZERO;
        }
        if (targetType == LocalDate.class) {
            return LocalDate.parse(node.asText());
        }
        if (targetType == LocalDateTime.class) {
            return LocalDateTime.parse(node.asText());
        }
        if (targetType == UUID.class) {
            return UUID.fromString(node.asText());
        }
        if (targetType.isEnum()) {
            @SuppressWarnings({"unchecked", "rawtypes"})
            Object[] constants = targetType.getEnumConstants();
            String text = node.asText();
            for (Object c : constants) {
                if (((Enum<?>) c).name().equals(text)) {
                    return c;
                }
            }
            throw new IllegalArgumentException("非法枚举值: " + text);
        }
        // 复杂 POJO 回退到 Jackson
        try {
            return MAPPER.treeToValue(node, targetType);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalArgumentException("反序列化参数失败: " + node, e);
        }
    }

    private Object defaultValueFor(Class<?> targetType) {
        if (!targetType.isPrimitive()) {
            return null;
        }
        if (targetType == boolean.class) return false;
        if (targetType == byte.class) return (byte) 0;
        if (targetType == short.class) return (short) 0;
        if (targetType == int.class) return 0;
        if (targetType == long.class) return 0L;
        if (targetType == float.class) return 0f;
        if (targetType == double.class) return 0d;
        if (targetType == char.class) return '\0';
        return null;
    }
}
