package org.yuca.ai;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.yuca.ai.agent.Agent;
import org.yuca.ai.agent.ChatContext;
import org.yuca.ai.agent.ToolManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Agent 核心测试：普通对话、工具调用
 */
@ExtendWith(MockitoExtension.class)
class AgentTest {

    @Mock
    private ChatModel chatModel;

    private Agent simpleAgent;

    @BeforeEach
    void setUp() {
        simpleAgent = Agent.builder()
                .chatModel(chatModel)
                .build();
    }

    // ==================== 普通对话 ====================

    @Test
    @DisplayName("普通对话 - 返回 AI 回复")
    void simpleChat_returnsResponse() {
        String reply = "你好！我是AI助手。";
        when(chatModel.chat(any(ChatRequest.class)))
                .thenReturn(ChatResponse.builder().aiMessage(AiMessage.from(reply)).build());

        ChatRequest request = ChatRequest.builder()
                .messages(List.of(UserMessage.from("你是谁")))
                .build();

        ChatResponse response = simpleAgent.execute(request, new ChatContext());

        assertThat(response).isNotNull();
        assertThat(response.aiMessage().text()).isEqualTo(reply);
        verify(chatModel, times(1)).chat(any(ChatRequest.class));
    }

    @Test
    @DisplayName("普通对话 - 请求包含用户消息")
    void simpleChat_requestContainsUserMessage() {
        when(chatModel.chat(any(ChatRequest.class)))
                .thenReturn(ChatResponse.builder().aiMessage(AiMessage.from("ok")).build());

        ChatRequest request = ChatRequest.builder()
                .messages(List.of(UserMessage.from("测试消息")))
                .build();

        simpleAgent.execute(request, new ChatContext());

        verify(chatModel).chat((ChatRequest) argThat((ChatRequest req) ->
                req.messages().stream()
                        .anyMatch(m -> m instanceof UserMessage u && u.singleText().equals("测试消息"))
        ));
    }

    // ==================== Agent 工具调用 ====================

    @Test
    @DisplayName("Agent 工具调用 - 模型调用工具后返回最终结果")
    void agentWithTool_toolCallThenFinalResponse() {
        ToolExecutionRequest toolReq = ToolExecutionRequest.builder()
                .id("call_1")
                .name("add")
                .arguments("{\"a\":3,\"b\":5}")
                .build();

        AiMessage toolCallMessage = AiMessage.from(List.of(toolReq));
        AiMessage finalMessage = AiMessage.from("3加5等于8");

        when(chatModel.chat(any(ChatRequest.class)))
                .thenReturn(ChatResponse.builder().aiMessage(toolCallMessage).build())
                .thenReturn(ChatResponse.builder().aiMessage(finalMessage).build());

        ToolManager toolManager = new ToolManager();
        toolManager.register(new TestCalculator());

        Agent agent = Agent.builder()
                .chatModel(chatModel)
                .toolManager(toolManager)
                .maxToolRounds(5)
                .build();

        ChatRequest request = ChatRequest.builder()
                .messages(List.of(UserMessage.from("3加5等于几")))
                .build();

        ChatResponse response = agent.execute(request, new ChatContext());

        assertThat(response.aiMessage().text()).isEqualTo("3加5等于8");
        verify(chatModel, times(2)).chat(any(ChatRequest.class));
    }

    @Test
    @DisplayName("Agent 工具调用 - 工具 schema 传递给模型")
    void agentWithTool_toolSpecificationsPassedToModel() {
        when(chatModel.chat(any(ChatRequest.class)))
                .thenReturn(ChatResponse.builder().aiMessage(AiMessage.from("不需要工具")).build());

        ToolManager toolManager = new ToolManager();
        toolManager.register(new TestCalculator());

        Agent agent = Agent.builder()
                .chatModel(chatModel)
                .toolManager(toolManager)
                .build();

        ChatRequest request = ChatRequest.builder()
                .messages(List.of(UserMessage.from("你好")))
                .build();

        agent.execute(request, new ChatContext());

        verify(chatModel).chat((ChatRequest) argThat((ChatRequest req) ->
                req.toolSpecifications() != null && !req.toolSpecifications().isEmpty()
        ));
    }

    @Test
    @DisplayName("Agent 工具调用 - 不需要工具时直接返回")
    void agentWithTool_noToolNeeded() {
        when(chatModel.chat(any(ChatRequest.class)))
                .thenReturn(ChatResponse.builder().aiMessage(AiMessage.from("直接回答")).build());

        ToolManager toolManager = new ToolManager();
        toolManager.register(new TestCalculator());

        Agent agent = Agent.builder()
                .chatModel(chatModel)
                .toolManager(toolManager)
                .build();

        ChatRequest request = ChatRequest.builder()
                .messages(List.of(UserMessage.from("你好")))
                .build();

        ChatResponse response = agent.execute(request, new ChatContext());

        assertThat(response.aiMessage().text()).isEqualTo("直接回答");
        verify(chatModel, times(1)).chat(any(ChatRequest.class));
    }

    @Test
    @DisplayName("Agent 工具调用 - 中间消息存入 ChatContext")
    void agentWithTool_conversationStoredInContext() {
        ToolExecutionRequest toolReq = ToolExecutionRequest.builder()
                .id("call_1")
                .name("add")
                .arguments("{\"a\":1,\"b\":2}")
                .build();

        when(chatModel.chat(any(ChatRequest.class)))
                .thenReturn(ChatResponse.builder().aiMessage(AiMessage.from(List.of(toolReq))).build())
                .thenReturn(ChatResponse.builder().aiMessage(AiMessage.from("结果是3")).build());

        ToolManager toolManager = new ToolManager();
        toolManager.register(new TestCalculator());

        Agent agent = Agent.builder()
                .chatModel(chatModel)
                .toolManager(toolManager)
                .build();

        ChatContext context = new ChatContext();
        agent.execute(ChatRequest.builder()
                .messages(List.of(UserMessage.from("1+2"))).build(), context);

        @SuppressWarnings("unchecked")
        List<ChatMessage> conversation = context.attribute("_agentConversation");
        assertThat(conversation).isNotNull();
        // 应包含：user + ai(tool call) + tool_result + ai(final, 非工具调用，不在messages中)
        assertThat(conversation.size()).isGreaterThanOrEqualTo(3);
    }

    // ==================== 真实调用集成测试 ====================

    @Test
    @Tag("integration")
    @DisplayName("真实调用 - Dashscope Qwen 普通对话")
    void realChat_dashscope() {
        ChatModel realModel = QwenChatModel.builder()
                .apiKey("sk-4632c16e41c64e738d3b4147aa58581f")
                .modelName("qwen3.5-flash")
                .build();

        Agent realAgent = Agent.builder()
                .chatModel(realModel)
                .build();

        ChatRequest request = ChatRequest.builder()
                .messages(List.of(UserMessage.from("用一句话介绍你自己")))
                .build();

        ChatResponse response = realAgent.execute(request, new ChatContext());

        assertThat(response).isNotNull();
        assertThat(response.aiMessage()).isNotNull();
        assertThat(response.aiMessage().text()).isNotBlank();
        System.out.println("AI 回复: " + response.aiMessage().text());
    }

    @Test
    @Tag("integration")
    @DisplayName("真实调用 - Dashscope Qwen 多轮对话")
    void realChat_dashscope_multiTurn() {
        ChatModel realModel = QwenChatModel.builder()
                .apiKey("sk-4632c16e41c64e738d3b4147aa58581f")
                .modelName("qwen3.5-flash")
                .build();

        Agent realAgent = Agent.builder()
                .chatModel(realModel)
                .build();

        // 第一轮
        ChatRequest request1 = ChatRequest.builder()
                .messages(List.of(UserMessage.from("记住我的名字叫小明")))
                .build();
        ChatResponse response1 = realAgent.execute(request1, new ChatContext());
        assertThat(response1.aiMessage().text()).isNotBlank();
        System.out.println("第一轮: " + response1.aiMessage().text());

        // 第二轮（带第一轮的上下文）
        ChatRequest request2 = ChatRequest.builder()
                .messages(List.of(
                        UserMessage.from("记住我的名字叫小明"),
                        response1.aiMessage(),
                        UserMessage.from("我叫什么名字？")
                ))
                .build();
        ChatResponse response2 = realAgent.execute(request2, new ChatContext());
        assertThat(response2.aiMessage().text()).isNotBlank();
        assertThat(response2.aiMessage().text()).contains("小明");
        System.out.println("第二轮: " + response2.aiMessage().text());
    }

    // ==================== 测试工具类 ====================

    static class TestCalculator {
        @Tool("两数相加")
        int add(int a, int b) {
            return a + b;
        }

        @Tool("两数相乘")
        int multiply(int a, int b) {
            return a * b;
        }
    }
}
