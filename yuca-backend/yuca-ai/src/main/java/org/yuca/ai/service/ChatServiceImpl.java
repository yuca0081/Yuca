package org.yuca.ai.service;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.yuca.ai.agent.Agent;
import org.yuca.ai.agent.ChatContext;
import org.yuca.ai.agent.enhancer.MemoryEnhancer;
import org.yuca.ai.agent.enhancer.SystemPromptEnhancer;
import org.yuca.ai.config.AiProperties;
import org.yuca.ai.memory.ChatMemoryStore;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.List;
import java.util.function.Consumer;

/**
 * 统一聊天服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatModel chatModel;
    private final StreamingChatModel streamingChatModel;
    private final ChatMemoryStore memoryStore;
    private final AiProperties aiProperties;

    private Agent simpleAgent;
    private Agent memoryAgent;

    @PostConstruct
    void init() {
        simpleAgent = Agent.builder()
                .chatModel(chatModel)
                .build();

        memoryAgent = Agent.builder()
                .chatModel(chatModel)
                .enhancer(new SystemPromptEnhancer("你是一个友好的AI助手，能够记住之前的对话内容。请用简洁、准确的方式回答用户的问题。"))
                .enhancer(new MemoryEnhancer(memoryStore, aiProperties.getMemory().getMaxMessages()))
                .build();
    }

    @Override
    public String chat(String message) {
        ChatRequest request = ChatRequest.builder()
                .messages(List.of(UserMessage.from(message)))
                .build();
        ChatResponse response = simpleAgent.execute(request, new ChatContext());
        return response.aiMessage().text();
    }

    @Override
    public ChatResponse chatWithMemory(String message, String sessionId) {
        ChatContext context = new ChatContext();
        context.setSessionId(sessionId);

        ChatRequest request = ChatRequest.builder()
                .messages(List.of(UserMessage.from(message)))
                .build();

        return memoryAgent.execute(request, context);
    }

    @Override
    public Flux<String> streamChat(String message) {
        return doStreamChat(
                ChatRequest.builder().messages(List.of(UserMessage.from(message))).build(),
                null, null
        );
    }

    @Override
    public Flux<String> streamChat(ChatRequest request,
                                   Consumer<String> tokenConsumer,
                                   Consumer<ChatResponse> responseConsumer) {
        return doStreamChat(request, tokenConsumer, responseConsumer);
    }

    private Flux<String> doStreamChat(ChatRequest request,
                                      Consumer<String> tokenConsumer,
                                      Consumer<ChatResponse> responseConsumer) {
        Sinks.Many<String> sink = Sinks.many().multicast().onBackpressureBuffer();

        streamingChatModel.chat(request, new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String partialResponse) {
                if (partialResponse != null && !partialResponse.isEmpty()) {
                    sink.tryEmitNext(partialResponse);
                    if (tokenConsumer != null) tokenConsumer.accept(partialResponse);
                }
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                if (responseConsumer != null) responseConsumer.accept(completeResponse);
                sink.tryEmitComplete();
            }

            @Override
            public void onError(Throwable error) {
                log.error("流式输出错误", error);
                sink.tryEmitError(error);
            }
        });

        return sink.asFlux();
    }
}
