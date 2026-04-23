package org.yuca.ai.agent.enhancer;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.yuca.ai.agent.ChatContext;
import org.yuca.ai.skill.SkillExecutor;

import java.util.ArrayList;
import java.util.List;

/**
 * 技能增强器
 * 将 Skill 模板展开后追加到当前对话（Inline 模式）
 * 保留原始用户消息，Skill prompt 作为额外 UserMessage 注入
 */
public class SkillEnhancer implements ChatEnhancer {

    private static final String SKILL_PROMPT_KEY = "_skillPrompt";

    private final SkillExecutor executor;
    private final String skillName;
    private final String arguments;

    public SkillEnhancer(SkillExecutor executor, String skillName, String arguments) {
        this.executor = executor;
        this.skillName = skillName;
        this.arguments = arguments;
    }

    @Override
    public ChatRequest before(ChatRequest request, ChatContext context) {
        String resolved = executor.resolve(skillName, arguments);
        context.attribute(SKILL_PROMPT_KEY, resolved);

        // 追加 skill prompt，保留原始消息
        List<ChatMessage> messages = new ArrayList<>(request.messages());
        messages.add(UserMessage.from(resolved));

        return ChatRequest.builder().messages(messages).build();
    }

    @Override
    public void after(ChatResponse response, ChatContext context) {
        // 无操作
    }
}
