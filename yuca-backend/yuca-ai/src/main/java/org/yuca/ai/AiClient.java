package org.yuca.ai;

import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.community.model.dashscope.QwenStreamingChatModel;

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.AiServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.yuca.ai.service.Assistant;
import reactor.core.publisher.Flux;

import java.util.function.Consumer;

/**
 * AI Client - Unified interface for AI capabilities using LangChain4j framework
 */
@Slf4j
@Component
public class AiClient {

    /**
     * Simple chat - accepts a String message and returns String response
     *
     * @param message the user message
     * @return AI response as String
     */
    public String simpleChat(String message) {
        if (message == null || message.trim().isEmpty()) {
            throw new IllegalArgumentException("Message cannot be null or empty");
        }
        try {
            QwenChatModel model = QwenChatModel.builder()
                    .apiKey("sk-4632c16e41c64e738d3b4147aa58581f")
                    .modelName("qwen3.5-plus")
                    .build();
            return model.chat(message);
        } catch (Exception e) {
            log.error("Error in simple chat", e);
            throw new RuntimeException("Failed to get AI response: " + e.getMessage(), e);
        }
    }

    /**
     * Full chat - accepts ChatRequest and returns ChatResponse
     *
     * @param request the chat request with all parameters
     * @return detailed chat response
     */
    public ChatResponse chat(ChatRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("ChatRequest cannot be null");
        }
        if (request.messages() == null ) {
            throw new IllegalArgumentException("Message cannot be null or empty");
        }

        try {
            QwenChatModel model = QwenChatModel.builder()
                    .apiKey("sk-4632c16e41c64e738d3b4147aa58581f")
                    .modelName("qwen3.5-flash")
                    .build();
            return model.chat(request);
        } catch (Exception e) {
            log.error("Error in chat", e);
            throw new RuntimeException("Failed to get AI response: " + e.getMessage(), e);
        }
    }

    /**
     * Stream chat with response callback
     *
     * @param request the chat request
     * @param tokenConsumer consumer for streaming tokens
     * @param responseConsumer consumer for final response (with token usage)
     */
    public Flux<String> streamChat(ChatRequest request, Consumer<String> tokenConsumer, Consumer<ChatResponse> responseConsumer) {

        StreamingChatModel model = QwenStreamingChatModel.builder()
                .apiKey("sk-4632c16e41c64e738d3b4147aa58581f")
                .modelName("qwen3.5-flash")
                .build();
        Assistant assistant = AiServices.create(Assistant.class, model);
        Flux<String> chat = assistant.streamChat(request)
                .doOnNext(token -> {
                    log.info("流式输出: {}", token);
                    System.out.print(token);
                })
                .doOnComplete(() -> {
                    log.info("流式输出完成");
                    System.out.println("\n[完成]");
                })
                .doOnError(error -> {
                    log.error("流式输出错误", error);
                });
        return chat;
    }

    /**
     * Stream chat (without response callback)
     *
     * @param request the chat request
     * @param tokenConsumer consumer for streaming tokens
     */
    public void streamChat(ChatRequest request, Consumer<String> tokenConsumer) {
        streamChat(request, tokenConsumer, null);
    }
}