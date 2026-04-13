package org.yuca.ai.skill.hooks;

import org.yuca.ai.skill.core.SkillDefinition;

/**
 * 技能钩子接口
 * 用于在技能执行的不同阶段插入自定义逻辑
 */
public interface SkillHook {

    /**
     * 执行前钩子
     */
    default void beforeExecution(SkillDefinition skill, SkillHookContext context) {
        // 默认不做任何操作
    }

    /**
     * 执行后钩子
     */
    default void afterExecution(
            SkillDefinition skill,
            SkillHookContext context,
            Object result) {
        // 默认不做任何操作
    }

    /**
     * 错误钩子
     */
    default void onError(
            SkillDefinition skill,
            SkillHookContext context,
            Exception exception) {
        // 默认不做任何操作
    }

    /**
     * 钩子上下文
     */
    interface SkillHookContext {
        String getSessionId();

        String getSkillName();

        void setAttribute(String key, Object value);

        <T> T getAttribute(String key);
    }
}
