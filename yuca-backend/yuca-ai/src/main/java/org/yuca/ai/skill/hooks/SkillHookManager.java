package org.yuca.ai.skill.hooks;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.yuca.ai.skill.core.SkillDefinition;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 技能钩子管理器
 * 管理所有技能钩子的注册和执行
 */
@Slf4j
@Component
public class SkillHookManager {

    /**
     * 全局钩子列表
     */
    private final List<SkillHook> globalHooks = new CopyOnWriteArrayList<>();

    /**
     * 技能特定钩子映射
     */
    private final Map<String, List<SkillHook>> skillSpecificHooks = new ConcurrentHashMap<>();

    /**
     * 注册全局钩子
     */
    public void registerGlobalHook(SkillHook hook) {
        globalHooks.add(hook);
        log.debug("注册全局钩子: {}", hook.getClass().getSimpleName());
    }

    /**
     * 注册技能特定钩子
     */
    public void registerSkillHook(String skillName, SkillHook hook) {
        skillSpecificHooks
                .computeIfAbsent(skillName, k -> new CopyOnWriteArrayList<>())
                .add(hook);
        log.debug("注册技能钩子: skill={}, hook={}", skillName, hook.getClass().getSimpleName());
    }

    /**
     * 执行前置钩子
     */
    public void executeBeforeHooks(SkillDefinition skill, SkillHookContext context) {
        // 执行全局钩子
        for (SkillHook hook : globalHooks) {
            try {
                hook.beforeExecution(skill, context);
            } catch (Exception e) {
                log.error("全局前置钩子执行失败: {}", hook.getClass().getSimpleName(), e);
            }
        }

        // 执行技能特定钩子
        List<SkillHook> hooks = skillSpecificHooks.get(skill.getMetadata().getName());
        if (hooks != null) {
            for (SkillHook hook : hooks) {
                try {
                    hook.beforeExecution(skill, context);
                } catch (Exception e) {
                    log.error("技能前置钩子执行失败: {}", hook.getClass().getSimpleName(), e);
                }
            }
        }
    }

    /**
     * 执行后置钩子
     */
    public void executeAfterHooks(
            SkillDefinition skill,
            SkillHookContext context,
            Object result) {

        // 执行全局钩子
        for (SkillHook hook : globalHooks) {
            try {
                hook.afterExecution(skill, context, result);
            } catch (Exception e) {
                log.error("全局后置钩子执行失败: {}", hook.getClass().getSimpleName(), e);
            }
        }

        // 执行技能特定钩子
        List<SkillHook> hooks = skillSpecificHooks.get(skill.getMetadata().getName());
        if (hooks != null) {
            for (SkillHook hook : hooks) {
                try {
                    hook.afterExecution(skill, context, result);
                } catch (Exception e) {
                    log.error("技能后置钩子执行失败: {}", hook.getClass().getSimpleName(), e);
                }
            }
        }
    }

    /**
     * 执行错误钩子
     */
    public void executeErrorHooks(
            SkillDefinition skill,
            SkillHookContext context,
            Exception exception) {

        // 执行全局钩子
        for (SkillHook hook : globalHooks) {
            try {
                hook.onError(skill, context, exception);
            } catch (Exception e) {
                log.error("全局错误钩子执行失败: {}", hook.getClass().getSimpleName(), e);
            }
        }

        // 执行技能特定钩子
        List<SkillHook> hooks = skillSpecificHooks.get(skill.getMetadata().getName());
        if (hooks != null) {
            for (SkillHook hook : hooks) {
                try {
                    hook.onError(skill, context, exception);
                } catch (Exception e) {
                    log.error("技能错误钩子执行失败: {}", hook.getClass().getSimpleName(), e);
                }
            }
        }
    }

    /**
     * 清除所有钩子
     */
    public void clearAllHooks() {
        globalHooks.clear();
        skillSpecificHooks.clear();
        log.info("所有钩子已清除");
    }
}
