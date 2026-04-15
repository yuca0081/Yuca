package org.yuca.ai.service;

import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import reactor.core.publisher.Flux;

import java.util.function.Consumer;

/**
 * 统一聊天服务接口
 */
public interface ChatService {

    /**
     * 简单同步对话
     */
    String chat(String message);

    /**
     * 带记忆的同步对话
     */
    ChatResponse chatWithMemory(String message, String sessionId);

    /**
     * 流式对话
     */
    Flux<String> streamChat(String message);

    /**
     * 带 SSE 回调的流式对话（供 yuca-assistant 使用）
     */
    Flux<String> streamChat(ChatRequest request,
                            Consumer<String> tokenConsumer,
                            Consumer<ChatResponse> responseConsumer);
}
