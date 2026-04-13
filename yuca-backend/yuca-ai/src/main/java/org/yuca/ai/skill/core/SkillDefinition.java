package org.yuca.ai.skill.core;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.service.tool.ToolExecutionRequest;

import java.util.List;

/**
 * 技能定义接口
 * 所有技能必须实现此接口
 */
public interface SkillDefinition {

    /**
     * 获取技能元数据
     */
    SkillMetadata getMetadata();

    /**
     * 生成提示词
     *
     * @param args 用户参数
     * @param context 执行上下文
     * @return 提示词消息列表
     */
    List<ChatMessage> generatePrompt(String args, SkillExecutionContext context);

    /**
     * 验证技能是否可用
     */
    default boolean isEnabled() {
        return true;
    }

    /**
     * 执行前的钩子
     */
    default void beforeExecution(SkillExecutionContext context) {
        // 默认不做任何操作
    }

    /**
     * 执行后的钩子
     */
    default void afterExecution(SkillExecutionContext context, String result) {
        // 默认不做任何操作
    }

    /**
     * 技能执行上下文
     */
    interface SkillExecutionContext {
        String getSessionId();

        String getProjectPath();

        List<ToolExecutionRequest> getToolExecutions();

        Object getAppState();

        void setAttribute(String key, Object value);

        <T> T getAttribute(String key);
    }
}
