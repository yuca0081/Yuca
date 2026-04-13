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
 * 调试技能
 * 帮助用户调试问题
 */
@Slf4j
@Component
public class DebugSkill implements ApplicationListener<ContextRefreshedEvent> {

    private final SkillRegistry skillRegistry;

    public DebugSkill(SkillRegistry skillRegistry) {
        this.skillRegistry = skillRegistry;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        registerDebugSkill();
    }

    private void registerDebugSkill() {
        SkillDefinition debugSkill = new SkillDefinition() {
            @Override
            public SkillMetadata getMetadata() {
                return SkillMetadata.builder()
                        .name("debug")
                        .description("调试当前会话中的问题，读取会话日志并帮助诊断问题")
                        .whenToUse("当用户遇到技术问题、错误或异常行为时使用")
                        .argumentHint("[问题描述]")
                        .allowedTools(Set.of("Read", "Grep", "Glob"))
                        .userInvocable(true)
                        .disableModelInvocation(true)
                        .source(SkillMetadata.SkillSource.BUNDLED)
                        .type(SkillMetadata.SkillType.PROMPT)
                        .build();
            }

            @Override
            public List<ChatMessage> generatePrompt(String args, SkillExecutionContext context) {
                String prompt = """
                        # 调试技能

                        ## 目标
                        帮助用户调试他们在当前会话中遇到的问题。

                        ## 问题描述
                        %s

                        ## 指示
                        1. 仔细阅读用户的问题描述
                        2. 检查相关的日志文件和错误信息
                        3. 分析问题的根本原因
                        4. 提供清晰的解释和解决方案
                        5. 如果需要更多信息，请向用户询问

                        ## 可用工具
                        - Read: 读取文件内容
                        - Grep: 搜索日志中的错误和警告
                        - Glob: 查找相关文件

                        请开始分析和诊断问题。
                        """.formatted(args != null ? args : "用户未描述具体问题");

                return List.of(
                        new SystemMessage("你是一个专业的调试助手。"),
                        new UserMessage(prompt)
                );
            }

            @Override
            public void beforeExecution(SkillExecutionContext context) {
                log.info("开始执行调试技能，会话ID: {}", context.getSessionId());
            }

            @Override
            public void afterExecution(SkillExecutionContext context, String result) {
                log.info("调试技能执行完成，会话ID: {}", context.getSessionId());
            }
        };

        skillRegistry.register(debugSkill);
        log.info("调试技能已注册");
    }
}
