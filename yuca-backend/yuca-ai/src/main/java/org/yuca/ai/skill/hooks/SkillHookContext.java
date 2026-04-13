package org.yuca.ai.skill.hooks;

import dev.langchain4j.service.tool.ToolExecutionRequest;
import org.yuca.ai.skill.core.SkillDefinition;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 默认的钩子上下文实现
 */
public class SkillHookContext implements SkillHook.SkillHookContext {

    private final String sessionId;
    private final String skillName;
    private final Map<String, Object> attributes = new HashMap<>();

    public SkillHookContext(String sessionId, String skillName) {
        this.sessionId = sessionId;
        this.skillName = skillName;
    }

    public static SkillHookContext create(SkillDefinition skill, String sessionId) {
        return new SkillHookContext(sessionId, skill.getMetadata().getName());
    }

    @Override
    public String getSessionId() {
        return sessionId;
    }

    @Override
    public String getSkillName() {
        return skillName;
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
