package org.yuca.ai.skill.core;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.yuca.ai.skill.resolver.MarkdownSkillParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 技能加载器
 * 负责从不同来源加载技能
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SkillLoader {

    private final SkillRegistry skillRegistry;
    private final MarkdownSkillParser markdownSkillParser;

    /**
     * 应用启动时自动加载技能
     */
    @EventListener(ApplicationReadyEvent.class)
    public void loadSkillsOnStartup() {
        log.info("开始加载技能...");

        // 加载内置技能
        loadBundledSkills();

        // 加载项目技能
        loadProjectSkills();

        // 加载用户技能
        loadUserSkills();

        var stats = skillRegistry.getStatistics();
        log.info("技能加载完成: {}", stats);
    }

    /**
     * 加载内置技能
     */
    private void loadBundledSkills() {
        log.debug("加载内置技能...");
        // 内置技能通过 Java 类直接注册
        // 在各个 Skill 类的 @PostConstruct 中调用 skillRegistry.register()
    }

    /**
     * 加载项目技能
     */
    private void loadProjectSkills() {
        log.debug("加载项目技能...");
        Path projectSkillsPath = Path.of(".claude/skills");
        loadSkillsFromDirectory(projectSkillsPath, SkillMetadata.SkillSource.PROJECT);
    }

    /**
     * 加载用户技能
     */
    private void loadUserSkills() {
        log.debug("加载用户技能...");
        String userHome = System.getProperty("user.home");
        Path userSkillsPath = Path.of(userHome, ".claude", "skills");
        loadSkillsFromDirectory(userSkillsPath, SkillMetadata.SkillSource.USER);
    }

    /**
     * 从目录加载技能
     * 支持 skill-name/SKILL.md 格式
     */
    private void loadSkillsFromDirectory(Path skillsDir, SkillMetadata.SkillSource source) {
        if (!Files.exists(skillsDir)) {
            log.debug("技能目录不存在: {}", skillsDir);
            return;
        }

        try {
            Files.list(skillsDir)
                    .filter(Files::isDirectory)
                    .forEach(skillDir -> loadSingleSkill(skillDir, source));
        } catch (IOException e) {
            log.error("加载技能目录失败: {}", skillsDir, e);
        }
    }

    /**
     * 加载单个技能
     */
    private void loadSingleSkill(Path skillDir, SkillMetadata.SkillSource source) {
        Path skillFile = skillDir.resolve("SKILL.md");
        if (!Files.exists(skillFile)) {
            log.debug("技能文件不存在: {}", skillFile);
            return;
        }

        try {
            String skillName = skillDir.getFileName().toString();
            String content = Files.readString(skillFile);

            markdownSkillParser.parseAndRegister(
                    skillName,
                    content,
                    skillDir.toString(),
                    source
            );

            log.debug("成功加载技能: {}", skillName);
        } catch (Exception e) {
            log.error("加载技能失败: {}", skillDir, e);
        }
    }

    /**
     * 动态发现并加载技能
     * 当操作文件时，自动发现并加载相关的项目级技能
     */
    public List<SkillDefinition> discoverSkillsForFiles(List<String> filePaths, String projectPath) {
        log.debug("为文件发现技能: {}", filePaths);

        // 实现类似于 Claude Code 的动态发现逻辑
        // 从文件路径向上遍历，查找 .claude/skills 目录

        return List.of(); // TODO: 实现动态发现逻辑
    }
}
