package org.yuca.ai.skill.core;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.service.tool.ToolExecutionRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.yuca.ai.skill.hooks.SkillHook;
import org.yuca.ai.skill.hooks.SkillHookManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 技能执行器
 * 负责执行技能并管理执行生命周期
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SkillExecutor {

    private final SkillRegistry skillRegistry;
    private final SkillHookManager hookManager;

    /**
     * 执行技能
     *
     * @param skillName 技能名称
     * @param args 用户参数
     * @param context 执行上下文
     * @return 执行结果
     */
    public SkillExecutionResult execute(
            String skillName,
            String args,
            SkillExecutionContext context) {

        // 查找技能
        SkillDefinition skill = skillRegistry.getSkill(skillName)
                .orElseThrow(() -> new IllegalArgumentException("技能不存在: " + skillName));

        // 检查技能是否可用
        if (!skill.isEnabled()) {
            return SkillExecutionResult.failure("技能未启用: " + skillName);
        }

        SkillMetadata metadata = skill.getMetadata();

        log.info("执行技能: {}, 参数: {}", skillName, args);

        try {
            // 执行前置钩子
            hookManager.executeBeforeHooks(skill, context);

            // 执行技能的前置回调
            skill.beforeExecution(context);

            // 生成提示词
            List<ChatMessage> messages = skill.generatePrompt(args, context);

            // 构建结果
            SkillExecutionResult result = SkillExecutionResult.success()
                    .messages(messages)
                    .allowedTools(metadata.getAllowedTools())
                    .metadata(metadata)
                    .build();

            // 执行后置钩子
            skill.afterExecution(context, "success");
            hookManager.executeAfterHooks(skill, context, result);

            log.info("技能执行成功: {}", skillName);
            return result;

        } catch (Exception e) {
            log.error("技能执行失败: {}", skillName, e);

            // 执行失败钩子
            hookManager.executeErrorHooks(skill, context, e);

            return SkillExecutionResult.failure("执行失败: " + e.getMessage());
        }
    }

    /**
     * 批量执行技能
     */
    public Map<String, SkillExecutionResult> executeBatch(
            Map<String, String> skillArgs,
            SkillExecutionContext context) {

        Map<String, SkillExecutionResult> results = new HashMap<>();

        for (Map.Entry<String, String> entry : skillArgs.entrySet()) {
            String skillName = entry.getKey();
            String args = entry.getValue();

            try {
                SkillExecutionResult result = execute(skillName, args, context);
                results.put(skillName, result);
            } catch (Exception e) {
                log.error("批量执行失败，技能: {}", skillName, e);
                results.put(skillName, SkillExecutionResult.failure(e.getMessage()));
            }
        }

        return results;
    }

    /**
     * 技能执行结果
     */
    public record SkillExecutionResult(
            boolean success,
            String errorMessage,
            List<ChatMessage> messages,
            java.util.Set<String> allowedTools,
            SkillMetadata metadata
    ) {
        public static Builder success() {
            return new Builder(true);
        }

        public static SkillExecutionResult failure(String errorMessage) {
            return new Builder(false)
                    .errorMessage(errorMessage)
                    .build();
        }

        public static class Builder {
            private final boolean success;
            private String errorMessage;
            private List<ChatMessage> messages;
            private java.util.Set<String> allowedTools;
            private SkillMetadata metadata;

            private Builder(boolean success) {
                this.success = success;
            }

            public Builder errorMessage(String errorMessage) {
                this.errorMessage = errorMessage;
                return this;
            }

            public Builder messages(List<ChatMessage> messages) {
                this.messages = messages;
                return this;
            }

            public Builder allowedTools(java.util.Set<String> allowedTools) {
                this.allowedTools = allowedTools;
                return this;
            }

            public Builder metadata(SkillMetadata metadata) {
                this.metadata = metadata;
                return this;
            }

            public SkillExecutionResult build() {
                return new SkillExecutionResult(
                        success,
                        errorMessage,
                        messages,
                        allowedTools,
                        metadata
                );
            }
        }
    }

    /**
     * 默认的技能执行上下文实现
     */
    public static class DefaultSkillExecutionContext implements SkillExecutionContext {
        private final String sessionId;
        private final String projectPath;
        private final Map<String, Object> attributes = new HashMap<>();

        public DefaultSkillExecutionContext(String sessionId, String projectPath) {
            this.sessionId = sessionId;
            this.projectPath = projectPath;
        }

        @Override
        public String getSessionId() {
            return sessionId;
        }

        @Override
        public String getProjectPath() {
            return projectPath;
        }

        @Override
        public List<ToolExecutionRequest> getToolExecutions() {
            return List.of();
        }

        @Override
        public Object getAppState() {
            return null;
        }

        @Override
        public void setAttribute(String key, Object value) {
            attributes.put(key, value);
        }

        @Override
        public <T> T getAttribute(String key) {
            return (T) attributes.get(key);
        }
    }
}
