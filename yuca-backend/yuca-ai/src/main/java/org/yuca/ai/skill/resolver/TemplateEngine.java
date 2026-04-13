package org.yuca.ai.skill.resolver;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 模板引擎
 * 用于替换提示词中的变量
 */
@Component
public class TemplateEngine {

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{([a-zA-Z0-9_]+)\\}");

    /**
     * 替换变量
     *
     * @param template 模板字符串
     * @param variables 变量映射
     * @return 替换后的字符串
     */
    public String replaceVariables(String template, Map<String, Object> variables) {
        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String variableName = matcher.group(1);
            Object value = variables.get(variableName);

            if (value != null) {
                matcher.appendReplacement(result, Matcher.quoteReplacement(value.toString()));
            } else {
                // 保留原变量
                matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group(0)));
            }
        }

        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * 替换单个变量
     */
    public String replaceVariable(String template, String name, String value) {
        return replaceVariables(template, Map.of(name, value));
    }
}
