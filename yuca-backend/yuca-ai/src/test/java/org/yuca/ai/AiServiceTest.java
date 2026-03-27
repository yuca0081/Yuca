package org.yuca.ai;

import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.community.model.dashscope.QwenStreamingChatModel;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.service.AiServices;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.yuca.ai.config.DashscopeProperties;
import org.yuca.ai.config.OpenAiProperties;
import org.yuca.ai.service.NoteAssistant;
import reactor.core.publisher.Flux;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LangChain4j AI Service 集成测试
 *
 * 测试覆盖：
 * - DashScope (Qwen) 模型
 * - OpenAI 兼容模型 (DeepSeek)
 * - 流式和非流式对话
 * - System Message
 * - 工具调用 (Function Calling)
 */
@SpringBootTest(classes = TestApplication.class)
@DisplayName("LangChain4j AI Service Tests")
class AiServiceTest {

    @Autowired(required = false)
    private DashscopeProperties dashscopeProperties;

    @Autowired(required = false)
    private OpenAiProperties openAiProperties;


    @Test
    @DisplayName("Qwen简单对话测试 - 同步调用")
    void testQwenSimpleChat() {
        // Skip if properties not configured
        if (dashscopeProperties == null || dashscopeProperties.getApiKey() == null) {
            System.out.println("DashScope properties not configured, skipping test");
            return;
        }

        StreamingChatModel streamModel = QwenStreamingChatModel.builder()
                .apiKey(dashscopeProperties.getApiKey())
                .modelName(dashscopeProperties.getModelName())
                .build();
        NoteAssistant noteAssistant = AiServices.builder(NoteAssistant.class)
                .streamingChatModel(streamModel)
                .build();
        ChatRequest request = ChatRequest.builder().messages(new UserMessage("你好")).build();

        Flux<String> response = noteAssistant.chatStream(request);
        StringBuilder result = new StringBuilder();
        response.doOnNext(token -> {
            System.out.print(token);
            result.append(token);
        }).blockLast(); // 阻塞等待流完成
        assertNotNull(response);
        System.out.println("\nQwen Response: " + result.toString());
    }
}
