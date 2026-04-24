package org.yuca.ai.client;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.StreamingChatModel;
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

    private final StreamingChatModel streamingChatModel;
    private final AgentFactory agentFactory;

    /**
     * 同步对话（无历史，适用于一次性调用如生成标题等）
     *
     * @param message 用户消息
     * @return AI回复文本
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
     *
     * @param message   用户消息
     * @param sessionId 会话ID，用于加载和保存对话历史
     * @return AI回复文本
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
     * 流式对话
     *
     * @param request          聊天请求
     * @param tokenConsumer    token回调（每个token触发一次）
     * @param responseConsumer 完成回调（流式结束时触发，含token统计）
     * @return Flux<String> token流
     */
    public Flux<String> streamChat(ChatRequest request,
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
