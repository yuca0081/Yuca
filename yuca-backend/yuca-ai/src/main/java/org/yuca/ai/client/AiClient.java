package org.yuca.ai.client;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.yuca.ai.agent.AgentFactory;
import org.yuca.ai.agent.ChatContext;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.List;
import java.util.function.Consumer;

/**
 * AI客户端
 * 供其他模块使用的统一AI能力入口
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiClient {

    private final AgentFactory agentFactory;

    /**
     * 同步对话（无历史，适用于一次性调用如生成标题等）
     */
    public String chat(String message) {
        ChatRequest request = ChatRequest.builder()
                .messages(List.of(UserMessage.from(message)))
                .build();
        ChatResponse response = agentFactory.simpleAgent().execute(request);
        return response.aiMessage().text();
    }

    /**
     * 同步对话（带对话历史）
     */
    public String chat(String message, String sessionId) {
        ChatContext context = new ChatContext();
        context.setSessionId(sessionId);

        ChatRequest request = ChatRequest.builder()
                .messages(List.of(UserMessage.from(message)))
                .build();

        ChatResponse response = agentFactory.defaultAgent(context).execute(request);
        return response.aiMessage().text();
    }

    /**
     * 流式对话（直接调用模型，不经过 Agent）
     */
    public Flux<String> streamChat(ChatRequest request,
                                   Consumer<String> tokenConsumer,
                                   Consumer<ChatResponse> responseConsumer) {
        Sinks.Many<String> sink = Sinks.many().multicast().onBackpressureBuffer();

        agentFactory.buildStreamingChatModel().chat(request, new StreamingChatResponseHandler() {
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
