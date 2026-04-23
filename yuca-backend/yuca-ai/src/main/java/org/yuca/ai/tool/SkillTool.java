package org.yuca.ai.tool;

import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.yuca.ai.skill.SkillExecutor;
import org.yuca.ai.skill.SkillRegistry;

/**
 * Skill 工具
 * 暴露给模型的工具，模型通过此工具发现和执行 Skill
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SkillTool {

    private final SkillRegistry registry;
    private final SkillExecutor executor;

    @Tool("列出所有可用的技能及其简短描述")
    public String listSkills() {
        var skills = registry.getAllSkills();
        if (skills.isEmpty()) {
            return "当前没有可用的技能";
        }

        StringBuilder sb = new StringBuilder("可用技能列表：\n");
        for (var skill : skills) {
            sb.append("- ").append(skill.getName())
                    .append(": ").append(skill.getDescription());
            if (skill.getArgumentHint() != null) {
                sb.append(" (参数: ").append(skill.getArgumentHint()).append(")");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    @Tool("执行指定的技能。name为技能名称，arguments为用户传入的参数")
    public String executeSkill(String name, String arguments) {
        log.info("执行 Skill: name={}, arguments={}", name, arguments);

        if (!registry.getSkill(name).isPresent()) {
            return "Skill 不存在: " + name + "。请先调用 listSkills 查看可用技能。";
        }

        try {
            String resolved = executor.resolve(name, arguments);
            log.info("Skill {} 模板解析完成，prompt 长度: {}", name, resolved.length());
            return resolved;
        } catch (Exception e) {
            log.error("Skill 执行失败: {}", name, e);
            return "Skill 执行失败: " + e.getMessage();
        }
    }
}
