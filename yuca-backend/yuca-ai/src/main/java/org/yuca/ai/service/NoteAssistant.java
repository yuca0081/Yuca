package org.yuca.ai.service;

import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.service.SystemMessage;
import reactor.core.publisher.Flux;

public interface NoteAssistant {
    @SystemMessage("你是一个笔记小助手")
    Flux<String> chatStream(ChatRequest request);
}
