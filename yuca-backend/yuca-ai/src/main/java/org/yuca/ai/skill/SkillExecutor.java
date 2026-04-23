package org.yuca.ai.skill;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Skill 执行器
 * 负责模板变量解析，供 SkillTool 和 SkillEnhancer 共用
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SkillExecutor {

    private final SkillRegistry registry;

    /**
     * 解析 skill 模板，返回替换变量后的 prompt
     *
     * @param name      skill 名称
     * @param arguments 用户传入的参数字符串
     * @return 解析后的 prompt
     */
    public String resolve(String name, String arguments) {
        SkillDefinition skill = registry.getSkill(name)
                .orElseThrow(() -> new IllegalArgumentException("Skill 不存在: " + name));
        return resolveTemplate(skill, arguments);
    }

    /**
     * 解析模板变量
     * 支持：$ARGUMENTS, $1 $2 位置参数, $paramName 命名参数
     */
    private String resolveTemplate(SkillDefinition skill, String arguments) {
        String prompt = skill.getPromptTemplate();

        if (arguments == null || arguments.isBlank()) {
            return prompt;
        }

        // 替换 $ARGUMENTS
        prompt = prompt.replace("$ARGUMENTS", arguments);

        // 位置参数 $1, $2, ...
        String[] args = arguments.split("\\s+");
        for (int i = 0; i < args.length; i++) {
            prompt = prompt.replace("$" + (i + 1), args[i]);
        }

        // 命名参数 $paramName
        List<String> paramNames = skill.getArguments();
        if (paramNames != null) {
            for (int i = 0; i < Math.min(args.length, paramNames.size()); i++) {
                prompt = prompt.replace("$" + paramNames.get(i), args[i]);
            }
        }

        return prompt;
    }
}
