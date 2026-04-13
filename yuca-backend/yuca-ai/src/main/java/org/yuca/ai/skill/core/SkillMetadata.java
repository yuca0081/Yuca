package org.yuca.ai.skill.core;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Set;

/**
 * 技能元数据
 * 定义技能的基本信息和配置
 */
@Data
@Builder
public class SkillMetadata {

    /**
     * 技能名称（唯一标识）
     */
    private String name;

    /**
     * 技能描述
     */
    private String description;

    /**
     * 使用场景说明
     */
    private String whenToUse;

    /**
     * 参数提示
     */
    private String argumentHint;

    /**
     * 允许使用的工具集合（空集合表示不限制）
     */
    @Builder.Default
    private Set<String> allowedTools = Set.of();

    /**
     * 是否用户可调用
     */
    @Builder.Default
    private boolean userInvocable = true;

    /**
     * 技能版本
     */
    private String version;

    /**
     * 是否启用模型调用
     */
    @Builder.Default
    private boolean disableModelInvocation = false;

    /**
     * 技能根目录（用于文件技能）
     */
    private String skillRoot;

    /**
     * 加载来源
     */
    private SkillSource source;

    /**
     * 技能类型
     */
    private SkillType type;

    /**
     * 技能执行上下文
     */
    private ExecutionContext context;

    /**
     * 代理类型（用于多智能体场景）
     */
    private String agent;

    /**
     * 路径模式（用于条件激活）
     */
    private List<String> pathPatterns;

    /**
     * 技能来源枚举
     */
    public enum SkillSource {
        BUNDLED,        // 内置
        USER,           // 用户目录
        PROJECT,        // 项目目录
        MANAGED,        // 托管
        PLUGIN          // 插件
    }

    /**
     * 技能类型枚举
     */
    public enum SkillType {
        PROMPT,         // 提示词技能
        TOOL,           // 工具技能
        AGENT           // 智能体技能
    }

    /**
     * 执行上下文枚举
     */
    public enum ExecutionContext {
        INLINE,         // 内联执行
        FORK            // 分叉执行
    }
}
