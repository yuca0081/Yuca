package org.yuca.ai.core.provider.qwen;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestClient;
import org.yuca.ai.core.model.ChatModel;
import org.yuca.ai.core.model.ChatRequest;
import org.yuca.ai.core.model.ChatResponse;
import org.yuca.ai.core.provider.openai.dto.ChatCompletionResponse;

/**
 * DashScope / Qwen 同步聊天模型。
 * 走 OpenAI 兼容端点 POST /compatible-mode/v1/chat/completions。
 */
@Slf4j
public class QwenChatModel implements ChatModel {

    private final RestClient restClient;
    private final String modelName;

    public QwenChatModel(String baseUrl, String apiKey, String modelName) {
        this.modelName = modelName;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        Object body = QwenConverters.toRequestBody(request, modelName, false);
        ChatCompletionResponse resp = restClient.post()
                .uri("/chat/completions")
                .body(body)
                .retrieve()
                .body(ChatCompletionResponse.class);
        return QwenConverters.toChatResponse(resp);
    }
}
