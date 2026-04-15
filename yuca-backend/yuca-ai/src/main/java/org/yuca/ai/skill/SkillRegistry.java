package org.yuca.ai.skill;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Skill 注册中心
 * 持有所有已加载的 skill 定义
 */
@Slf4j
@Component
public class SkillRegistry {

    private final Map<String, SkillDefinition> skills = new LinkedHashMap<>();

    /**
     * 注册一个 skill
     */
    public void register(SkillDefinition skill) {
        skills.put(skill.getName(), skill);
        log.info("注册 skill: {} - {}", skill.getName(), skill.getDescription());
    }

    /**
     * 按名称查找 skill
     */
    public Optional<SkillDefinition> getSkill(String name) {
        return Optional.ofNullable(skills.get(name));
    }

    /**
     * 获取所有 skill
     */
    public List<SkillDefinition> getAllSkills() {
        return List.copyOf(skills.values());
    }

    /**
     * 获取 skill 数量
     */
    public int size() {
        return skills.size();
    }
}
