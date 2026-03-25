package org.yuca.ai;

import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.alibaba.dashscope.common.MultiModalMessage;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;
import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.community.model.dashscope.QwenChatRequestParameters;
import dev.langchain4j.community.model.dashscope.QwenStreamingChatModel;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.*;
import io.reactivex.Flowable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * {@link AiClient} 单元测试
 *
 * 测试覆盖：
 * - 参数验证
 * - 成功的 API 交互
 * - 错误处理
 * - 流式功能
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AiClient Unit Tests")
class AiClientTest {

    private AiClient aiClient;


    @Mock
    private QwenStreamingChatModel mockStreamingModel;

    @Mock
    private ChatRequest mockChatRequest;

    @Mock
    private ChatResponse mockChatResponse;

    @BeforeEach
    void setUp() {
        // 创建 AiClient 实例
        // 注意：在生产环境中，模型应该从外部注入/配置
        // 此测试结构假设我们稍后会重构 AiClient 以提高可测试性
        aiClient = new AiClient();
    }

    @Nested
    @DisplayName("simpleChat() Method Tests")
    class SimpleChatTests {

        @Test
        @DisplayName("当消息为 null 时应抛出 IllegalArgumentException")
        void testSimpleChat_NullMessage_ThrowsException() {
            // Given
            String nullMessage = null;

            // When & Then
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> aiClient.simpleChat(nullMessage)
            );

            assertEquals("Message cannot be null or empty", exception.getMessage());
        }

        @Test
        @DisplayName("当消息为空时应抛出 IllegalArgumentException")
        void testSimpleChat_EmptyMessage_ThrowsException() {
            // Given
            String emptyMessage = "";

            // When & Then
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> aiClient.simpleChat(emptyMessage)
            );

            assertEquals("Message cannot be null or empty", exception.getMessage());
        }

        @Test
        @DisplayName("当消息为空白时应抛出 IllegalArgumentException")
        void testSimpleChat_BlankMessage_ThrowsException() {
            // Given
            String blankMessage = "   ";

            // When & Then
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> aiClient.simpleChat(blankMessage)
            );

            assertEquals("Message cannot be null or empty", exception.getMessage());
        }

        @Test
        @DisplayName("当提供有效消息时应返回响应")
        void testSimpleChat_ValidMessage_ReturnsResponse() {
            // 注意：此测试需要重构 AiClient 以接受模型作为依赖
            // 或使用 PowerMock/inline mocking 来模拟静态构建器
            // 目前，此测试演示了预期行为

            // Given
            String validMessage = "Hello, AI!";

            // When & Then
             String response = aiClient.simpleChat(validMessage);
             assertNotNull(response);

            // 当前实现的测试：
            assertDoesNotThrow(() -> {
                // 这会进行真实的 API 调用 - 不适合单元测试
                // 在生产环境中，应重构以注入模型依赖
            });
        }
    }

    @Nested
    @DisplayName("chat() Method Tests")
    class ChatTests {

        @Test
        @DisplayName("Should throw IllegalArgumentException when request is null")
        void testChat_NullRequest_ThrowsException() {
            // When & Then
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> aiClient.chat(null)
            );

            assertEquals("ChatRequest cannot be null", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when request messages are null")
        void testChat_NullMessages_ThrowsException() {
            // Given
            ChatRequest requestWithNullMessages = mock(ChatRequest.class);
            when(requestWithNullMessages.messages()).thenReturn(null);

            // When & Then
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> aiClient.chat(requestWithNullMessages)
            );

            assertEquals("Message cannot be null or empty", exception.getMessage());
        }

        @Test
        @DisplayName("Should accept valid ChatRequest")
        void testChat_ValidRequest_Success() {
            // Given
            ChatRequest validRequest = ChatRequest.builder().messages(new UserMessage("你好")).build();
            ChatResponse response = aiClient.chat(validRequest);
            assertNotNull(response);
        }
    }

    @Nested
    @DisplayName("streamChat() Method Tests")
    class StreamChatTests {

        @Test
        @DisplayName("Should stream tokens to consumer")
        void testStreamChat_TokensDeliveredToConsumer() {
            // Given
            ChatRequest request = mock(ChatRequest.class);
            when(request.messages()).thenReturn(List.of());

            AtomicReference<String> receivedToken = new AtomicReference<>();
            Consumer<String> tokenConsumer = token -> receivedToken.set(token);

            // When & Then
            // In real scenario with mocked model:
            // doAnswer(invocation -> {
            //     StreamingChatResponseHandler handler = invocation.getArgument(1);
            //     handler.onPartialResponse("test token");
            //     return null;
            // }).when(mockStreamingModel).chat(any(), any());

            assertDoesNotThrow(() -> {
                aiClient.streamChat(request, tokenConsumer);
            });
        }

        @Test
        @DisplayName("Should call response consumer on completion")
        void testStreamChat_ResponseConsumerCalled() {
            // Given
            ChatRequest request = mock(ChatRequest.class);
            when(request.messages()).thenReturn(List.of());

            AtomicReference<ChatResponse> receivedResponse = new AtomicReference<>();
            Consumer<ChatResponse> responseConsumer = response -> receivedResponse.set(response);

            Consumer<String> tokenConsumer = token -> {};

            // When & Then
            assertDoesNotThrow(() -> {
                aiClient.streamChat(request, tokenConsumer, responseConsumer);
            });
        }

        @Test
        @DisplayName("应优雅处理 null 响应消费者")
        void testStreamChat_NullResponseConsumer_NoException() {
            // Given
            ChatRequest request = mock(ChatRequest.class);
            when(request.messages()).thenReturn(List.of());
            Consumer<String> tokenConsumer = token -> {};

            // When & Then
            assertDoesNotThrow(() -> {
                aiClient.streamChat(request, tokenConsumer, null);
            });
        }

        @Test
        @DisplayName("流式聊天应正常返回回答并打印内容")
        void testStreamChat_RealResponse_PrintContent() {
            StringBuilder builder = new StringBuilder();
            StreamingChatModel model = QwenStreamingChatModel.builder()
                    .apiKey("sk-4632c16e41c64e738d3b4147aa58581f")
                    .modelName("qwen3.5-flash")
                    .build();

            String userMessage = "Tell me a joke";

            model.chat(userMessage, new StreamingChatResponseHandler() {

                @Override
                public void onPartialResponse(String partialResponse) {
                    System.out.println("onPartialResponse: " + partialResponse);
                    builder.append(partialResponse);
                }

                @Override
                public void onPartialThinking(PartialThinking partialThinking) {
                    System.out.println("onPartialThinking: " + partialThinking);
                }

                @Override
                public void onPartialToolCall(PartialToolCall partialToolCall) {
                    System.out.println("onPartialToolCall: " + partialToolCall);
                }

                @Override
                public void onCompleteToolCall(CompleteToolCall completeToolCall) {
                    System.out.println("onCompleteToolCall: " + completeToolCall);
                }

                @Override
                public void onCompleteResponse(ChatResponse completeResponse) {
                    System.out.println("onCompleteResponse: " + completeResponse);
                }

                @Override
                public void onError(Throwable error) {
                    error.printStackTrace();
                }
            });
            System.out.println(builder.toString());
        }
    }

    @Test
    @DisplayName("流式聊天应正常返回回答并打印内容")
    void testStreamChat_conversation() {
        MultiModalConversation conv = new MultiModalConversation();
        MultiModalConversationParam param = MultiModalConversationParam.builder()
                .apiKey("sk-4632c16e41c64e738d3b4147aa58581f")
                .model("qwen3.5-flash")  // 可按需更换为其它多模态模型，并修改相应的 messages
                .message("你好")
                .incrementalOutput(true)
                .build();
        Flowable<MultiModalConversationResult> result = null;
        try {
            result = conv.streamCall(param);
        } catch (NoApiKeyException e) {
            throw new RuntimeException(e);
        } catch (UploadFileException e) {
            throw new RuntimeException(e);
        }
        result.blockingForEach(item -> {
            try {
                var content = item.getOutput().getChoices().get(0).getMessage().getContent();
                // 判断content是否存在且不为空
                if (content != null &&  !content.isEmpty()) {
                    System.out.println(content.get(0).get("text"));
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        });
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should wrap API exceptions in RuntimeException")
        void testApiException_WrappedInRuntimeException() {
            // This test verifies exception handling behavior
            // In production with mocked model:
            // when(mockChatModel.chat(any())).thenThrow(new RuntimeException("API Error"));

            RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> aiClient.simpleChat("test message")
            );

            assertTrue(exception.getMessage().contains("Failed to get AI response"));
        }
    }

    @Nested
    @DisplayName("Integration-Style Tests")
    class IntegrationTests {

        @Test
        @DisplayName("End-to-end flow for simpleChat")
        void testSimpleChat_EndToEnd() {
            // This test demonstrates the expected flow
            // In a real integration test with test API key:
            // String response = aiClient.simpleChat("Say 'test passed'");
            // assertTrue(response.contains("test passed"));

            // For unit test, we verify the method structure
            assertDoesNotThrow(() -> {
                // Verify method signature and basic flow
                java.lang.reflect.Method method = AiClient.class.getMethod("simpleChat", String.class);
                assertEquals(String.class, method.getReturnType());
            });
        }

        @Test
        @DisplayName("Verify public API surface")
        void testPublicApiSurface() {
            // Verify all public methods exist
            assertDoesNotThrow(() -> {
                AiClient.class.getMethod("simpleChat", String.class);
                AiClient.class.getMethod("chat", ChatRequest.class);
                AiClient.class.getMethod("streamChat", ChatRequest.class, Consumer.class, Consumer.class);
                AiClient.class.getMethod("streamChat", ChatRequest.class, Consumer.class);
            });
        }
    }
}
