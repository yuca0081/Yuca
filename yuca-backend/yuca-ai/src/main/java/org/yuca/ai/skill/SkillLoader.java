package org.yuca.ai.skill;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Skill 加载器
 * 启动时从 classpath:skills/ 目录扫描 SKILL.md 文件并解析注册
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SkillLoader {

    private final SkillRegistry skillRegistry;
    private final Yaml yaml = new Yaml();

    @PostConstruct
    public void loadSkills() {
        log.info("开始加载 skills...");
        try {
            var resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:skills/*/SKILL.md");

            for (Resource resource : resources) {
                try {
                    String content = readResource(resource);
                    SkillDefinition skill = parseSkillMd(content, resource.getURL().toString());
                    skillRegistry.register(skill);
                } catch (Exception e) {
                    log.error("加载 skill 失败: {}", resource.getURL(), e);
                }
            }

            log.info("skill 加载完成，共 {} 个", skillRegistry.size());
        } catch (IOException e) {
            log.warn("未找到 skills 目录或无 skill 文件");
        }
    }

    /**
     * 解析 SKILL.md 内容
     * 格式：--- yaml frontmatter --- markdown prompt 正文
     */
    @SuppressWarnings("unchecked")
    private SkillDefinition parseSkillMd(String content, String source) {
        String[] parts = splitFrontmatter(content);
        String frontmatter = parts[0];
        String body = parts[1].trim();

        Map<String, Object> yamlMap = new HashMap<>();
        if (!frontmatter.isBlank()) {
            yamlMap = yaml.load(frontmatter);
        }

        String name = getString(yamlMap, "name");
        if (name == null || name.isBlank()) {
            name = extractNameFromSource(source);
        }

        return SkillDefinition.builder()
                .name(name)
                .description(getString(yamlMap, "description"))
                .whenToUse(getString(yamlMap, "when_to_use"))
                .argumentHint(getString(yamlMap, "argument-hint"))
                .arguments(getStringList(yamlMap, "arguments"))
                .allowedTools(new HashSet<>(getStringList(yamlMap, "allowed-tools")))
                .promptTemplate(body)
                .source(source)
                .build();
    }

    /**
     * 分离 frontmatter 和正文
     */
    private String[] splitFrontmatter(String content) {
        String trimmed = content.trim();
        if (trimmed.startsWith("---")) {
            int endIndex = trimmed.indexOf("---", 3);
            if (endIndex > 0) {
                String frontmatter = trimmed.substring(3, endIndex).trim();
                String body = trimmed.substring(endIndex + 3).trim();
                return new String[]{frontmatter, body};
            }
        }
        return new String[]{"", trimmed};
    }

    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    @SuppressWarnings("unchecked")
    private List<String> getStringList(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof List<?> list) {
            return list.stream().map(Object::toString).toList();
        }
        return List.of();
    }

    /**
     * 从资源路径提取 skill 名称
     */
    private String extractNameFromSource(String source) {
        int skillsIdx = source.lastIndexOf("skills/");
        if (skillsIdx >= 0) {
            String sub = source.substring(skillsIdx + "skills/".length());
            int slash = sub.indexOf("/");
            if (slash > 0) {
                return sub.substring(0, slash);
            }
        }
        return "unknown";
    }

    private String readResource(Resource resource) throws IOException {
        try (var reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        }
    }
}
