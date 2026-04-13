package org.yuca.ai.skill.bundled;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;
import org.yuca.ai.skill.core.SkillDefinition;
import org.yuca.ai.skill.core.SkillMetadata;
import org.yuca.ai.skill.core.SkillRegistry;

import java.util.List;
import java.util.Set;

/**
 * 代码审查技能
 * 对代码进行审查和改进建议
 */
@Slf4j
@Component
public class CodeReviewSkill implements ApplicationListener<ContextRefreshedEvent> {

    private final SkillRegistry skillRegistry;

    public CodeReviewSkill(SkillRegistry skillRegistry) {
        this.skillRegistry = skillRegistry;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        registerCodeReviewSkill();
    }

    private void registerCodeReviewSkill() {
        SkillDefinition codeReviewSkill = new SkillDefinition() {
            @Override
            public SkillMetadata getMetadata() {
                return SkillMetadata.builder()
                        .name("code-review")
                        .description("对代码进行审查，发现潜在问题并提供改进建议")
                        .whenToUse("当代码需要审查、重构或改进时使用")
                        .argumentHint("[文件路径或代码片段]")
                        .allowedTools(Set.of("Read", "Grep", "Glob"))
                        .userInvocable(true)
                        .source(SkillMetadata.SkillSource.BUNDLED)
                        .type(SkillMetadata.SkillType.PROMPT)
                        .build();
            }

            @Override
            public List<ChatMessage> generatePrompt(String args, SkillExecutionContext context) {
                String prompt = """
                        # 代码审查技能

                        ## 目标
                        对提供的代码进行全面审查，识别潜在问题并提供改进建议。

                        ## 审查范围
                        %s

                        ## 审查维度

                        ### 1. 代码质量
                        - 代码结构和组织
                        - 命名规范
                        - 代码重复
                        - 复杂度和可读性

                        ### 2. 最佳实践
                        - 语言特性使用
                        - 设计模式应用
                        - 异常处理
                        - 资源管理

                        ### 3. 安全性
                        - 输入验证
                        - 敏感数据处理
                        - 权限检查
                        - SQL 注入、XSS 等漏洞

                        ### 4. 性能
                        - 算法复杂度
                        - 资源使用
                        - 缓存策略
                        - 并发处理

                        ### 5. 可维护性
                        - 注释和文档
                        - 测试覆盖
                        - 错误处理
                        - 日志记录

                        ## 输出格式

                        请按照以下格式提供审查结果：

                        ### 总体评估
                        - 代码质量等级（优秀/良好/一般/需要改进）
                        - 主要优点
                        - 主要问题

                        ### 详细问题
                        按优先级列出发现的问题：
                        1. **[严重/中等/轻微]** 问题描述
                           - 位置
                           - 风险分析
                           - 改进建议

                        ### 改进建议
                        - 具体的改进措施
                        - 重构建议
                        - 最佳实践推荐

                        ### 示例代码
                        提供改进后的代码示例

                        请开始代码审查。
                        """.formatted(args != null ? args : "请提供需要审查的代码或文件路径");

                return List.of(
                        new SystemMessage("你是一个专业的代码审查专家，精通多种编程语言和最佳实践。"),
                        new UserMessage(prompt)
                );
            }
        };

        skillRegistry.register(codeReviewSkill);
        log.info("代码审查技能已注册");
    }
}
