package org.yuca.ai.agent.interceptor;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.yuca.ai.agent.ChatContext;
import org.yuca.ai.agent.ChatInterceptor;
import org.yuca.ai.skill.SkillDefinition;

import java.util.List;

/**
 * Skill 拦截器
 * 将 Skill 模板展开为 UserMessage
 */
public class SkillInterceptor implements ChatInterceptor {

    private final SkillDefinition skill;
    private final String arguments;

    public SkillInterceptor(SkillDefinition skill, String arguments) {
        this.skill = skill;
        this.arguments = arguments;
    }

    @Override
    public ChatRequest before(ChatRequest request, ChatContext context) {
        String prompt = skill.getPromptTemplate();

        // 替换 $ARGUMENTS
        if (arguments != null && !arguments.isBlank()) {
            prompt = prompt.replace("$ARGUMENTS", arguments);

            // 替换命名参数 $paramName
            if (skill.getArguments() != null) {
                String[] args = arguments.split("\\s+");
                List<String> paramNames = skill.getArguments();
                for (int i = 0; i < Math.min(args.length, paramNames.size()); i++) {
                    prompt = prompt.replace("$" + paramNames.get(i), args[i]);
                }
            }
        }

        return ChatRequest.builder()
                .messages(List.of(UserMessage.from(prompt)))
                .build();
    }

    @Override
    public void after(ChatResponse response, ChatContext context) {
        // 无操作
    }
}
