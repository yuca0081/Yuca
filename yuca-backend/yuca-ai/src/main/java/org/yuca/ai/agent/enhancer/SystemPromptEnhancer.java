package org.yuca.ai.agent.enhancer;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.yuca.ai.agent.ChatContext;

import java.util.ArrayList;
import java.util.List;

/**
 * 系统提示增强器
 * 在消息头部插入 SystemMessage
 */
public class SystemPromptEnhancer implements ChatEnhancer {

    private final String systemPrompt;

    public SystemPromptEnhancer(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    @Override
    public ChatRequest before(ChatRequest request, ChatContext context) {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(SystemMessage.from(systemPrompt));
        messages.addAll(request.messages());
        return ChatRequest.builder().messages(messages).build();
    }

    @Override
    public void after(ChatResponse response, ChatContext context) {
        // 无操作
    }
}
