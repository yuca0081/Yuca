package org.yuca.ai;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.yuca.ai.config.AiProperties;
import org.yuca.ai.memory.ChatMemoryStore;
import org.yuca.ai.service.ChatServiceImpl;
import reactor.core.publisher.Flux;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 流式对话测试
 */
@ExtendWith(MockitoExtension.class)
class StreamingChatTest {

    @Mock
    private StreamingChatModel streamingChatModel;

    @Mock
    private dev.langchain4j.model.chat.ChatModel chatModel;

    @Mock
    private ChatMemoryStore memoryStore;

    private ChatServiceImpl createService() {
        AiProperties.MemoryConfig cfg = new AiProperties.MemoryConfig();
        cfg.setMaxMessages(50);
        AiProperties props = new AiProperties();
        props.setMemory(cfg);
        ChatServiceImpl service = new ChatServiceImpl(chatModel, streamingChatModel, memoryStore, props);
        // init() 是包私有方法，通过反射调用
        try {
            Method init = ChatServiceImpl.class.getDeclaredMethod("init");
            init.setAccessible(true);
            init.invoke(service);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return service;
    }

    @Test
    @DisplayName("流式对话 - 逐 token 输出")
    void streamChat_emitsTokens() {
        doAnswer(invocation -> {
            StreamingChatResponseHandler handler = invocation.getArgument(1);
            handler.onPartialResponse("你");
            handler.onPartialResponse("好");
            handler.onPartialResponse("！");
            handler.onCompleteResponse(ChatResponse.builder()
                    .aiMessage(AiMessage.from("你好！")).build());
            return null;
        }).when(streamingChatModel).chat(any(ChatRequest.class), any(StreamingChatResponseHandler.class));

        ChatServiceImpl service = createService();
        Flux<String> flux = service.streamChat("你好");

        List<String> tokens = flux.collectList().block(Duration.ofSeconds(5));

        assertThat(tokens).containsExactly("你", "好", "！");
    }

    @Test
    @DisplayName("流式对话 - 空内容不发射")
    void streamChat_emptyTokenSkipped() {
        doAnswer(invocation -> {
            StreamingChatResponseHandler handler = invocation.getArgument(1);
            handler.onPartialResponse("");
            handler.onPartialResponse(null);
            handler.onPartialResponse("有内容");
            handler.onCompleteResponse(ChatResponse.builder()
                    .aiMessage(AiMessage.from("有内容")).build());
            return null;
        }).when(streamingChatModel).chat(any(ChatRequest.class), any(StreamingChatResponseHandler.class));

        ChatServiceImpl service = createService();
        Flux<String> flux = service.streamChat("test");

        List<String> tokens = flux.collectList().block(Duration.ofSeconds(5));

        assertThat(tokens).containsExactly("有内容");
    }

    @Test
    @DisplayName("流式对话 - 错误传播到 Flux")
    void streamChat_errorPropagated() {
        doAnswer(invocation -> {
            StreamingChatResponseHandler handler = invocation.getArgument(1);
            handler.onError(new RuntimeException("模型调用失败"));
            return null;
        }).when(streamingChatModel).chat(any(ChatRequest.class), any(StreamingChatResponseHandler.class));

        ChatServiceImpl service = createService();
        Flux<String> flux = service.streamChat("test");

        assertThatThrownBy(() -> flux.blockFirst(Duration.ofSeconds(5)))
                .hasMessageContaining("模型调用失败");
    }

    @Test
    @DisplayName("流式对话 - 带 Consumer 回调")
    void streamChat_withCallbacks() {
        doAnswer(invocation -> {
            StreamingChatResponseHandler handler = invocation.getArgument(1);
            handler.onPartialResponse("token1");
            handler.onPartialResponse("token2");
            handler.onCompleteResponse(ChatResponse.builder()
                    .aiMessage(AiMessage.from("token1token2")).build());
            return null;
        }).when(streamingChatModel).chat(any(ChatRequest.class), any(StreamingChatResponseHandler.class));

        ChatServiceImpl service = createService();

        StringBuilder tokenCollector = new StringBuilder();
        AtomicReference<ChatResponse> responseHolder = new AtomicReference<>();

        ChatRequest request = ChatRequest.builder()
                .messages(List.of(UserMessage.from("hi")))
                .build();

        Flux<String> flux = service.streamChat(request,
                tokenCollector::append,
                responseHolder::set);

        flux.collectList().block(Duration.ofSeconds(5));

        assertThat(tokenCollector.toString()).isEqualTo("token1token2");
        assertThat(responseHolder.get()).isNotNull();
        assertThat(responseHolder.get().aiMessage().text()).isEqualTo("token1token2");
    }
}
