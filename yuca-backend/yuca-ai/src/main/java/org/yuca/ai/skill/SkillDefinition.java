package org.yuca.ai.skill;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Set;

/**
 * Skill 定义数据类
 * 对应一个 SKILL.md 文件
 */
@Value
@Builder
public class SkillDefinition {

    /** skill 名称（目录名或 frontmatter 指定） */
    String name;

    /** 一句话描述 */
    String description;

    /** 使用场景提示 */
    String whenToUse;

    /** 参数提示 */
    String argumentHint;

    /** 声明的参数名列表 */
    List<String> arguments;

    /** 允许使用的工具白名单 */
    Set<String> allowedTools;

    /** prompt 模板正文（SKILL.md frontmatter 之后的内容） */
    String promptTemplate;

    /** 来源路径 */
    String source;
}
