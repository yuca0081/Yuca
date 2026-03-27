package org.yuca.ai;

import dev.langchain4j.community.model.dashscope.QwenStreamingChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.yuca.ai.config.DashscopeProperties;
import org.yuca.ai.service.NoteAssistant;

@Component
@ConditionalOnProperty(prefix = "langchain4j.dashscope.chat-model", name = "api-key")
public class AiServiceFactory {

    @Resource
    private DashscopeProperties dashscopeProperties;

    @Bean
    public NoteAssistant assistant(){
        StreamingChatModel streamModel = QwenStreamingChatModel.builder()
                .apiKey(dashscopeProperties.getApiKey())
                .modelName(dashscopeProperties.getModelName())
                .build();
        return AiServices.builder(NoteAssistant.class)
                .streamingChatModel(streamModel)
                .build();
    }
}
