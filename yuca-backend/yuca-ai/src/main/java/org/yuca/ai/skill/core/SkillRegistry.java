package org.yuca.ai.skill.core;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 技能注册表
 * 管理所有已注册的技能
 */
@Slf4j
@Component
public class SkillRegistry {

    /**
     * 按名称存储的技能映射
     */
    private final Map<String, SkillDefinition> skillsByName = new ConcurrentHashMap<>();

    /**
     * 按来源存储的技能映射
     */
    private final Map<SkillMetadata.SkillSource, List<SkillDefinition>> skillsBySource = new ConcurrentHashMap<>();

    /**
     * 条件技能（按路径模式激活）
     */
    private final Map<String, SkillDefinition> conditionalSkills = new ConcurrentHashMap<>();

    /**
     * 已激活的条件技能名称
     */
    private final Set<String> activatedConditionalSkills = ConcurrentHashMap.newKeySet();

    /**
     * 注册技能
     */
    public void register(SkillDefinition skill) {
        SkillMetadata metadata = skill.getMetadata();
        String name = metadata.getName();

        // 检查是否已注册
        if (skillsByName.containsKey(name)) {
            log.warn("技能 '{}' 已存在，将被覆盖", name);
        }

        skillsByName.put(name, skill);

        // 按来源分组
        skillsBySource
                .computeIfAbsent(metadata.getSource(), k -> new ArrayList<>())
                .add(skill);

        // 如果有路径模式，添加到条件技能
        if (metadata.getPathPatterns() != null && !metadata.getPathPatterns().isEmpty()) {
            if (!activatedConditionalSkills.contains(name)) {
                conditionalSkills.put(name, skill);
                log.debug("条件技能 '{}' 已注册，等待路径匹配激活", name);
            }
        }

        log.info("技能 '{}' 注册成功，来源: {}, 类型: {}",
                name, metadata.getSource(), metadata.getType());
    }

    /**
     * 获取技能
     */
    public Optional<SkillDefinition> getSkill(String name) {
        return Optional.ofNullable(skillsByName.get(name));
    }

    /**
     * 获取所有技能
     */
    public Collection<SkillDefinition> getAllSkills() {
        return Collections.unmodifiableCollection(skillsByName.values());
    }

    /**
     * 按来源获取技能
     */
    public List<SkillDefinition> getSkillsBySource(SkillMetadata.SkillSource source) {
        return skillsBySource.getOrDefault(source, List.of());
    }

    /**
     * 获取用户可调用的技能
     */
    public List<SkillDefinition> getUserInvocableSkills() {
        return skillsByName.values().stream()
                .filter(skill -> skill.getMetadata().isUserInvocable())
                .filter(SkillDefinition::isEnabled)
                .toList();
    }

    /**
     * 根据文件路径激活条件技能
     */
    public List<SkillDefinition> activateConditionalSkills(List<String> filePaths, String projectPath) {
        List<SkillDefinition> activated = new ArrayList<>();

        for (Map.Entry<String, SkillDefinition> entry : conditionalSkills.entrySet()) {
            String name = entry.getKey();
            SkillDefinition skill = entry.getValue();
            List<String> patterns = skill.getMetadata().getPathPatterns();

            // 检查是否有文件匹配路径模式
            boolean matches = filePaths.stream()
                    .anyMatch(filePath -> matchesPathPatterns(filePath, patterns, projectPath));

            if (matches) {
                // 从条件技能移到正常技能
                conditionalSkills.remove(name);
                activatedConditionalSkills.add(name);
                activated.add(skill);
                log.info("条件技能 '{}' 已激活（匹配文件路径）", name);
            }
        }

        return activated;
    }

    /**
     * 匹配文件路径模式
     */
    private boolean matchesPathPatterns(String filePath, List<String> patterns, String projectPath) {
        // 简化实现：使用通配符匹配
        // 实际可以使用 gitignore 风格的匹配库
        for (String pattern : patterns) {
            if (filePath.matches(convertGlobToRegex(pattern))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 将 glob 模式转换为正则表达式
     */
    private String convertGlobToRegex(String glob) {
        return glob
                .replace(".", "\\.")
                .replace("*", ".*")
                .replace("?", ".");
    }

    /**
     * 注销技能
     */
    public void unregister(String name) {
        SkillDefinition skill = skillsByName.remove(name);
        if (skill != null) {
            SkillMetadata.SkillSource source = skill.getMetadata().getSource();
            skillsBySource.getOrDefault(source, new ArrayList<>()).remove(skill);
            conditionalSkills.remove(name);
            activatedConditionalSkills.remove(name);
            log.info("技能 '{}' 已注销", name);
        }
    }

    /**
     * 清空所有技能
     */
    public void clear() {
        skillsByName.clear();
        skillsBySource.clear();
        conditionalSkills.clear();
        activatedConditionalSkills.clear();
        log.info("所有技能已清空");
    }

    /**
     * 获取统计信息
     */
    public Map<String, Object> getStatistics() {
        return Map.of(
                "totalSkills", skillsByName.size(),
                "userInvocableSkills", getUserInvocableSkills().size(),
                "conditionalSkills", conditionalSkills.size(),
                "activatedConditionalSkills", activatedConditionalSkills.size(),
                "skillsBySource", skillsBySource.entrySet().stream()
                        .collect(java.util.stream.Collectors.toMap(
                                e -> e.getKey().toString(),
                                e -> e.getValue().size()
                        ))
        );
    }
}
