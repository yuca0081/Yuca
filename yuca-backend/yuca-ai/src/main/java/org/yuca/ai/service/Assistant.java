package org.yuca.ai.service;

import dev.langchain4j.model.chat.request.ChatRequest;
import reactor.core.publisher.Flux;

public interface Assistant {
    String chat(String userMessage);

    Flux<String> streamChat(String userMessage);

    Flux<String> streamChat(ChatRequest request);
}
