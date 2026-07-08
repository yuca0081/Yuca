package org.yuca.ai.core.tool;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 工具注解，标注在方法上声明为 LLM 可调用工具。
 * 等价于 langchain4j 的 dev.langchain4j.agent.tool.Tool。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Tool {
    /** 工具描述，会作为提示词传给模型 */
    String value() default "";
}
