package org.yuca.ai.skill.resolver;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.yuca.ai.skill.core.SkillDefinition;
import org.yuca.ai.skill.core.SkillMetadata;
import org.yuca.ai.skill.core.SkillRegistry;

import java.util.List;
import java.util.Set;

/**
 * Markdown 技能解析器
 * 解析 SKILL.md 文件并注册技能
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MarkdownSkillParser {

    private final SkillRegistry skillRegistry;
    private final TemplateEngine templateEngine;

    /**
     * 解析 Markdown 并注册技能
     */
    public void parseAndRegister(
            String skillName,
            String content,
            String skillRoot,
            SkillMetadata.SkillSource source) {

        // 解析 frontmatter
        SkillFrontmatter frontmatter = parseFrontmatter(content);

        // 提取 markdown 内容
        String markdownContent = extractMarkdownContent(content);

        // 创建技能定义
        SkillDefinition skill = createSkillDefinition(
                skillName,
                frontmatter,
                markdownContent,
                skillRoot,
                source
        );

        // 注册技能
        skillRegistry.register(skill);
    }

    /**
     * 解析 frontmatter
     */
    private SkillFrontmatter parseFrontmatter(String content) {
        SkillFrontmatter frontmatter = new SkillFrontmatter();

        if (!content.startsWith("---")) {
            return frontmatter;
        }

        int end = content.indexOf("---", 4);
        if (end == -1) {
            return frontmatter;
        }

        String yamlContent = content.substring(4, end).trim();
        String[] lines = yamlContent.split("\n");

        for (String line : lines) {
            int colonIndex = line.indexOf(':');
            if (colonIndex == -1) continue;

            String key = line.substring(0, colonIndex).trim();
            String value = line.substring(colonIndex + 1).trim();

            switch (key) {
                case "name" -> frontmatter.setDisplayName(value);
                case "description" -> frontmatter.setDescription(value);
                case "when_to_use" -> frontmatter.setWhenToUse(value);
                case "version" -> frontmatter.setVersion(value);
                case "argument_hint" -> frontmatter.setArgumentHint(value);
                case "allowed_tools" -> frontmatter.setAllowedTools(Set.of(value.split(",\\s*")));
                case "user_invocable" -> frontmatter.setUserInvocable(Boolean.parseBoolean(value));
                case "paths" -> frontmatter.setPathPatterns(List.of(value.split(",\\s*")));
            }
        }

        return frontmatter;
    }

    /**
     * 提取 markdown 内容
     */
    private String extractMarkdownContent(String content) {
        if (!content.startsWith("---")) {
            return content;
        }

        int end = content.indexOf("---", 4);
        if (end == -1) {
            return content;
        }

        return content.substring(end + 3).trim();
    }

    /**
     * 创建技能定义
     */
    private SkillDefinition createSkillDefinition(
            String skillName,
            SkillFrontmatter frontmatter,
            String markdownContent,
            String skillRoot,
            SkillMetadata.SkillSource source) {

        return new SkillDefinition() {
            @Override
            public SkillMetadata getMetadata() {
                return SkillMetadata.builder()
                        .name(skillName)
                        .description(
                                frontmatter.getDescription() != null
                                        ? frontmatter.getDescription()
                                        : "技能: " + skillName
                        )
                        .whenToUse(frontmatter.getWhenToUse())
                        .argumentHint(frontmatter.getArgumentHint())
                        .allowedTools(frontmatter.getAllowedTools())
                        .userInvocable(frontmatter.isUserInvocable())
                        .version(frontmatter.getVersion())
                        .skillRoot(skillRoot)
                        .source(source)
                        .type(SkillMetadata.SkillType.PROMPT)
                        .pathPatterns(frontmatter.getPathPatterns())
                        .build();
            }

            @Override
            public List<ChatMessage> generatePrompt(String args, SkillExecutionContext context) {
                String prompt = markdownContent;

                // 添加基础目录信息
                if (skillRoot != null) {
                    prompt = "Base directory for this skill: " + skillRoot + "\n\n" + prompt;
                }

                // 替换参数
                if (args != null && !args.isEmpty()) {
                    prompt = templateEngine.replaceVariables(prompt, Map.of(
                            "args", args,
                            "sessionId", context.getSessionId(),
                            "projectPath", context.getProjectPath()
                    ));
                }

                return List.of(new SystemMessage(prompt));
            }

            @Override
            public boolean isEnabled() {
                return true;
            }
        };
    }

    /**
     * Skill Frontmatter 数据类
     */
    private static class SkillFrontmatter {
        private String displayName;
        private String description;
        private String whenToUse;
        private String version;
        private String argumentHint;
        private Set<String> allowedTools = Set.of();
        private boolean userInvocable = true;
        private List<String> pathPatterns = List.of();

        // Getters and Setters
        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getWhenToUse() { return whenToUse; }
        public void setWhenToUse(String whenToUse) { this.whenToUse = whenToUse; }

        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }

        public String getArgumentHint() { return argumentHint; }
        public void setArgumentHint(String argumentHint) { this.argumentHint = argumentHint; }

        public Set<String> getAllowedTools() { return allowedTools; }
        public void setAllowedTools(Set<String> allowedTools) { this.allowedTools = allowedTools; }

        public boolean isUserInvocable() { return userInvocable; }
        public void setUserInvocable(boolean userInvocable) { this.userInvocable = userInvocable; }

        public List<String> getPathPatterns() { return pathPatterns; }
        public void setPathPatterns(List<String> pathPatterns) { this.pathPatterns = pathPatterns; }
    }
}
