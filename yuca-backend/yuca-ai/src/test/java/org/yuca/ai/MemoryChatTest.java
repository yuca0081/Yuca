package org.yuca.ai;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.yuca.ai.agent.Agent;
import org.yuca.ai.agent.ChatContext;
import org.yuca.ai.agent.enhancer.MemoryEnhancer;
import org.yuca.ai.agent.enhancer.SystemPromptEnhancer;
import org.yuca.ai.memory.ChatMemoryStore;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 带记忆的对话测试
 */
@ExtendWith(MockitoExtension.class)
class MemoryChatTest {

    @Mock
    private ChatModel chatModel;

    @Mock
    private ChatMemoryStore memoryStore;

    @Test
    @DisplayName("记忆对话 - 第一次对话无历史")
    void firstChat_noHistory() {
        when(memoryStore.getMessages("session-1")).thenReturn(List.of());
        when(chatModel.chat(any(ChatRequest.class)))
                .thenReturn(ChatResponse.builder().aiMessage(AiMessage.from("你好！")).build());

        Agent agent = Agent.builder()
                .chatModel(chatModel)
                .enhancer(new MemoryEnhancer(memoryStore, 50))
                .build();

        ChatContext context = new ChatContext();
        context.setSessionId("session-1");

        ChatRequest request = ChatRequest.builder()
                .messages(List.of(UserMessage.from("你好")))
                .build();

        ChatResponse response = agent.execute(request, context);

        assertThat(response.aiMessage().text()).isEqualTo("你好！");
        verify(memoryStore).appendMessages(eq("session-1"), (List<ChatMessage>) argThat(msgs -> {
            @SuppressWarnings("unchecked")
            List<ChatMessage> list = (List<ChatMessage>) msgs;
            return list.size() == 2
                    && list.get(0) instanceof UserMessage
                    && list.get(1) instanceof AiMessage;
        }));
    }

    @Test
    @DisplayName("记忆对话 - 第二次对话加载历史")
    void secondChat_loadsHistory() {
        List<ChatMessage> history = List.of(
                UserMessage.from("我叫小明"),
                AiMessage.from("你好小明！")
        );
        when(memoryStore.getMessages("session-1")).thenReturn(history);
        when(chatModel.chat(any(ChatRequest.class)))
                .thenReturn(ChatResponse.builder().aiMessage(AiMessage.from("小明你好，我记得你")).build());

        Agent agent = Agent.builder()
                .chatModel(chatModel)
                .enhancer(new MemoryEnhancer(memoryStore, 50))
                .build();

        ChatContext context = new ChatContext();
        context.setSessionId("session-1");

        ChatRequest request = ChatRequest.builder()
                .messages(List.of(UserMessage.from("你还记得我吗")))
                .build();

        agent.execute(request, context);

        verify(chatModel).chat((ChatRequest) argThat((ChatRequest req) -> {
            List<ChatMessage> msgs = req.messages();
            return msgs.size() == 3
                    && ((UserMessage) msgs.get(0)).singleText().equals("我叫小明")
                    && ((AiMessage) msgs.get(1)).text().equals("你好小明！")
                    && ((UserMessage) msgs.get(2)).singleText().equals("你还记得我吗");
        }));
    }

    @Test
    @DisplayName("记忆对话 - 超过 maxMessages 时截断旧消息")
    void historyTruncated_exceedsMax() {
        List<ChatMessage> history = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            history.add(UserMessage.from("问题" + i));
            history.add(AiMessage.from("回答" + i));
        }

        when(memoryStore.getMessages("session-1")).thenReturn(history);
        when(chatModel.chat(any(ChatRequest.class)))
                .thenReturn(ChatResponse.builder().aiMessage(AiMessage.from("好的")).build());

        Agent agent = Agent.builder()
                .chatModel(chatModel)
                .enhancer(new MemoryEnhancer(memoryStore, 10))
                .build();

        ChatContext context = new ChatContext();
        context.setSessionId("session-1");

        agent.execute(ChatRequest.builder()
                .messages(List.of(UserMessage.from("新问题"))).build(), context);

        // 只发送了最后 10 条历史 + 1 条新消息 = 11
        verify(chatModel).chat((ChatRequest) argThat((ChatRequest req) -> req.messages().size() == 11));
    }

    @Test
    @DisplayName("记忆对话 - 无 sessionId 不加载也不保存")
    void noSessionId_noMemoryInteraction() {
        when(chatModel.chat(any(ChatRequest.class)))
                .thenReturn(ChatResponse.builder().aiMessage(AiMessage.from("ok")).build());

        Agent agent = Agent.builder()
                .chatModel(chatModel)
                .enhancer(new MemoryEnhancer(memoryStore, 50))
                .build();

        ChatContext context = new ChatContext();

        agent.execute(ChatRequest.builder()
                .messages(List.of(UserMessage.from("hi"))).build(), context);

        verify(memoryStore, never()).getMessages(any());
        verify(memoryStore, never()).appendMessages(any(), any());
    }

    @Test
    @DisplayName("记忆对话 - SystemMessage 不保存到 store")
    void systemMessageFiltered() {
        when(memoryStore.getMessages("session-1")).thenReturn(List.of());
        when(chatModel.chat(any(ChatRequest.class)))
                .thenReturn(ChatResponse.builder().aiMessage(AiMessage.from("好的")).build());

        Agent agent = Agent.builder()
                .chatModel(chatModel)
                .enhancer(new SystemPromptEnhancer("你是助手"))
                .enhancer(new MemoryEnhancer(memoryStore, 50))
                .build();

        ChatContext context = new ChatContext();
        context.setSessionId("session-1");

        agent.execute(ChatRequest.builder()
                .messages(List.of(UserMessage.from("hi"))).build(), context);

        verify(memoryStore).appendMessages(eq("session-1"), (List<ChatMessage>) argThat(msgs -> {
            @SuppressWarnings("unchecked")
            List<ChatMessage> list = (List<ChatMessage>) msgs;
            return list.stream().noneMatch(m -> m instanceof dev.langchain4j.data.message.SystemMessage);
        }));
    }

    @Test
    @DisplayName("记忆对话 - 多轮对话历史累积")
    void multiTurn_historyAccumulates() {
        List<ChatMessage> accumulatedHistory = List.of(
                UserMessage.from("第一轮问题"),
                AiMessage.from("第一轮回答"),
                UserMessage.from("第二轮问题"),
                AiMessage.from("第二轮回答")
        );
        when(memoryStore.getMessages("session-1")).thenReturn(accumulatedHistory);
        when(chatModel.chat(any(ChatRequest.class)))
                .thenReturn(ChatResponse.builder().aiMessage(AiMessage.from("第三轮回答")).build());

        Agent agent = Agent.builder()
                .chatModel(chatModel)
                .enhancer(new MemoryEnhancer(memoryStore, 50))
                .build();

        ChatContext context = new ChatContext();
        context.setSessionId("session-1");

        agent.execute(ChatRequest.builder()
                .messages(List.of(UserMessage.from("第三轮问题"))).build(), context);

        // 4 history + 1 new = 5
        verify(chatModel).chat((ChatRequest) argThat((ChatRequest req) -> req.messages().size() == 5));
        // save: user + ai = 2
        verify(memoryStore).appendMessages(eq("session-1"), (List<ChatMessage>) argThat(msgs -> {
            @SuppressWarnings("unchecked")
            List<ChatMessage> list = (List<ChatMessage>) msgs;
            return list.size() == 2;
        }));
    }
}
