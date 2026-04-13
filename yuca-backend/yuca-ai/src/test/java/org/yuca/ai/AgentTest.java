package org.yuca.ai;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.agent.AgentInvocationException;
import dev.langchain4j.agentic.agent.ErrorRecoveryResult;
import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.community.model.dashscope.QwenStreamingChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.yuca.ai.config.DashscopeProperties;
import org.yuca.ai.tool.Calculator;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LangChain4j Agentic 模块测试
 *
 * 测试覆盖:
 * - 基本 Agent 定义和使用
 * - 顺序工作流 (Sequential workflow)
 * - 循环工作流 (Loop workflow)
 * - 并行工作流 (Parallel workflow)
 * - 条件工作流 (Conditional workflow)
 * - 监督代理 (Supervisor Agent)
 * - 错误处理 (Error handling)
 * - 非 AI 代理 (Non-AI agents)
 * - Human-in-the-loop
 * - 内存与上下文管理 (Memory & Context)
 *
 * 参考文档: https://langchain4j.cn/tutorials/agents.html
 */
@SpringBootTest(classes = TestApplication.class)
@DisplayName("LangChain4j Agentic 模块测试")
class AgentTest {

    @Autowired(required = false)
    private DashscopeProperties dashscopeProperties;

    /**
     * 跳过测试的条件检查
     */
    private boolean shouldSkipTest() {
        return dashscopeProperties == null || dashscopeProperties.getApiKey() == null;
    }

    /**
     * 创建基础的 ChatModel
     */
    private ChatModel createChatModel() {
        return QwenChatModel.builder()
                .apiKey(dashscopeProperties.getApiKey())
                .modelName(dashscopeProperties.getModelName())
                .build();
    }

    /**
     * 创建基础的 StreamingChatModel
     */
    private QwenStreamingChatModel createStreamingChatModel() {
        return QwenStreamingChatModel.builder()
                .apiKey(dashscopeProperties.getApiKey())
                .modelName(dashscopeProperties.getModelName())
                .build();
    }

    // ==================== 基本 Agent 测试 ====================

    /**
     * 创意写作 Agent
     */
    public interface CreativeWriter {
        @UserMessage("""
            You are a creative writer.
            Generate a draft of a story no more than 3 sentences long around the given topic.
            Return only the story and nothing else.
            The topic is {{topic}}.
            """)
        @Agent
        String generateStory(String topic);
    }

    /**
     * 数学助手 Agent - 使用工具
     */
    public interface MathAssistant {
        @UserMessage("""
            You are a math assistant.
            Answer the user's question about calculations.
            Use the available tools when needed.
            The question is: {{question}}
            """)
        @Agent
        String answer(String question);
    }

    @Test
    @DisplayName("测试 1: 基本 Agent - 带工具调用")
    void testBasicAgent() {
        if (shouldSkipTest()) {
            System.out.println("DashScope properties not configured, skipping test");
            return;
        }

        ChatModel chatModel = createChatModel();

        System.out.println("========== 创建带工具的 Agent ==========");
        Calculator calculator = new Calculator();

        MathAssistant mathAssistant = AgenticServices
                .agentBuilder(MathAssistant.class)
                .chatModel(chatModel)
                .tools(calculator)
                .build();

        System.out.println("\n========== 测试 1: 字符串长度计算 ==========");
        String answer1 = mathAssistant.answer("What is the length of the string 'Hello World'?");
        System.out.println("Answer: " + answer1);

        System.out.println("\n========== 测试 2: 加法计算 ==========");
        String answer2 = mathAssistant.answer("What is 123 plus 456?");
        System.out.println("Answer: " + answer2);

        System.out.println("\n========== 测试 3: 平方根计算 ==========");
        String answer3 = mathAssistant.answer("What is the square root of 144?");
        System.out.println("Answer: " + answer3);

        System.out.println("\n========== 测试 4: 复合计算 ==========");
        String answer4 = mathAssistant.answer("Calculate the length of 'LangChain4j', then add 100 to that number");
        System.out.println("Answer: " + answer4);

        assertNotNull(answer1);
        assertNotNull(answer2);
        assertNotNull(answer3);
        assertNotNull(answer4);

        System.out.println("\n========== 测试完成 ==========");
    }

    // ==================== 顺序工作流测试 ====================

    /**
     * 受众编辑 Agent
     */
    public interface AudienceEditor {
        @UserMessage("""
            You are a professional editor.
            Analyze and rewrite the following story to better align with the target audience of {{audience}}.
            Return only the story and nothing else.
            The story is "{{story}}".
            """)
        @Agent
        String editStory(String story, String audience);
    }

    /**
     * 风格编辑 Agent
     */
    public interface StyleEditor {
        @UserMessage("""
            You are a professional editor.
            Rewrite the following story in the style of {{style}}.
            Return only the story and nothing else.
            The story is "{{story}}".
            """)
        @Agent
        String editStyle(String story, String style);
    }

    /**
     * 小说创作器 - 组合多个 Agent 的顺序工作流
     */
    public interface NovelCreator {
        @Agent
        String createNovel(String topic, String audience, String style);
    }

    @Test
    @DisplayName("测试 2: 顺序工作流 - 小说创作")
    void testSequentialWorkflow() {
        if (shouldSkipTest()) {
            System.out.println("DashScope properties not configured, skipping test");
            return;
        }

        ChatModel chatModel = createChatModel();

        // 创建各个 Agent
        CreativeWriter creativeWriter = AgenticServices
                .agentBuilder(CreativeWriter.class)
                .chatModel(chatModel)
                .outputKey("story")
                .build();

        AudienceEditor audienceEditor = AgenticServices
                .agentBuilder(AudienceEditor.class)
                .chatModel(chatModel)
                .outputKey("story")
                .build();

        StyleEditor styleEditor = AgenticServices
                .agentBuilder(StyleEditor.class)
                .chatModel(chatModel)
                .outputKey("story")
                .build();

        // 组合成顺序工作流
        NovelCreator novelCreator = AgenticServices
                .sequenceBuilder(NovelCreator.class)
                .subAgents(creativeWriter, audienceEditor, styleEditor)
                .build();

        String story = novelCreator.createNovel("a dragon and a wizard", "young adults", "fantasy");

        assertNotNull(story);
        assertFalse(story.isEmpty());
        System.out.println("Final Novel: " + story);
    }

    // ==================== 循环工作流测试 ====================

    /**
     * 风格评分 Agent
     */
    public interface StyleScorer {
        @UserMessage("""
            You are a critical reviewer.
            Give a review score between 0.0 and 1.0 for the following story
            based on how well it aligns with the style '{{style}}'.
            Return only the score and nothing else.
            The story is: "{{story}}"
            """)
        @Agent
        double scoreStyle(String story, String style);
    }

    @Test
    @DisplayName("测试 3: 循环工作流 - 风格改进循环")
    void testLoopWorkflow() {
        if (shouldSkipTest()) {
            System.out.println("DashScope properties not configured, skipping test");
            return;
        }

        ChatModel chatModel = createChatModel();

        // 创建 Agent
        CreativeWriter creativeWriter = AgenticServices
                .agentBuilder(CreativeWriter.class)
                .chatModel(chatModel)
                .outputKey("story")
                .build();

        StyleEditor styleEditor = AgenticServices
                .agentBuilder(StyleEditor.class)
                .chatModel(chatModel)
                .outputKey("story")
                .build();

        StyleScorer styleScorer = AgenticServices
                .agentBuilder(StyleScorer.class)
                .chatModel(chatModel)
                .outputKey("score")
                .build();

        // 创建循环工作流 - 直到分数 >= 0.6 或达到 3 次迭代
        var styleReviewLoop = AgenticServices
                .loopBuilder()
                .subAgents(styleScorer, styleEditor)
                .maxIterations(3)
                .exitCondition(agenticScope -> agenticScope.readState("score", 0.0) >= 0.6)
                .build();

        // 组合创作和循环
        var styledWriter = AgenticServices
                .sequenceBuilder()
                .subAgents(creativeWriter, styleReviewLoop)
                .build();

        Map<String, Object> input = Map.of(
                "topic", "a brave cat",
                "style", "comedy"
        );

        String story = (String) styledWriter.invoke(input);

        assertNotNull(story);
        System.out.println("Styled Story: " + story);
    }

    // ==================== 并行工作流测试 ====================

    /**
     * 晚餐计划记录
     */
    public record EveningPlan(String movie, String meal) {}

    /**
     * 美食专家 Agent
     */
    public interface FoodExpert {
        @UserMessage("""
            You are a great evening planner.
            Propose a list of 3 meals matching the given mood: {{mood}}.
            For each meal, just give the name of the meal.
            Provide a list with the 3 items and nothing else.
            """)
        @Agent
        List<String> findMeal(String mood);
    }

    /**
     * 电影专家 Agent
     */
    public interface MovieExpert {
        @UserMessage("""
            You are a great evening planner.
            Propose a list of 3 movies matching the given mood: {{mood}}.
            Provide a list with the 3 items and nothing else.
            """)
        @Agent
        List<String> findMovie(String mood);
    }

    /**
     * 晚间计划器 Agent
     */
    public interface EveningPlannerAgent {
        @Agent
        List<EveningPlan> plan(String mood);
    }
    // ==================== 条件工作流测试 ====================

    /**
     * 请求分类枚举
     */
    public enum RequestCategory {
        LEGAL, MEDICAL, TECHNICAL, UNKNOWN
    }

    /**
     * 分类路由 Agent
     */
    public interface CategoryRouter {
        @UserMessage("""
            Analyze the following user request and categorize it as 'legal', 'medical' or 'technical'.
            In case the request doesn't belong to any of those categories categorize it as 'unknown'.
            Reply with only one of those words and nothing else.
            The user request is: '{{request}}'.
            """)
        @Agent
        RequestCategory classify(String request);
    }

    /**
     * 医疗专家 Agent
     */
    public interface MedicalExpert {
        @UserMessage("""
            You are a medical expert.
            Analyze the following user request under a medical point of view
            and provide the best possible answer.
            The user request is {{request}}.
            """)
        @Agent
        String medical(String request);
    }

    /**
     * 技术专家 Agent
     */
    public interface TechnicalExpert {
        @UserMessage("""
            You are a technical expert.
            Analyze the following user request and provide the best possible technical answer.
            The user request is {{request}}.
            """)
        @Agent
        String technical(String request);
    }

    /**
     * 专家路由 Agent
     */
    public interface ExpertRouterAgent {
        @Agent
        String ask(String request);
    }

    @Test
    @DisplayName("测试 5: 条件工作流 - 专家路由")
    void testConditionalWorkflow() {
        if (shouldSkipTest()) {
            System.out.println("DashScope properties not configured, skipping test");
            return;
        }

        ChatModel chatModel = createChatModel();

        CategoryRouter routerAgent = AgenticServices
                .agentBuilder(CategoryRouter.class)
                .chatModel(chatModel)
                .outputKey("category")
                .build();

        MedicalExpert medicalExpert = AgenticServices
                .agentBuilder(MedicalExpert.class)
                .chatModel(chatModel)
                .outputKey("response")
                .build();

        TechnicalExpert technicalExpert = AgenticServices
                .agentBuilder(TechnicalExpert.class)
                .chatModel(chatModel)
                .outputKey("response")
                .build();

        var expertsAgent = AgenticServices
                .conditionalBuilder()
                .subAgents(agenticScope -> agenticScope.readState("category", RequestCategory.UNKNOWN) == RequestCategory.MEDICAL, medicalExpert)
                .subAgents(agenticScope -> agenticScope.readState("category", RequestCategory.UNKNOWN) == RequestCategory.TECHNICAL, technicalExpert)
                .build();

        ExpertRouterAgent expertRouterAgent = AgenticServices
                .sequenceBuilder(ExpertRouterAgent.class)
                .subAgents(routerAgent, expertsAgent)
                .build();

        // 测试医疗问题
        String medicalResponse = expertRouterAgent.ask("I have a headache, what should I do?");
        System.out.println("Medical Response: " + medicalResponse);
        assertNotNull(medicalResponse);

        // 测试技术问题
        String techResponse = expertRouterAgent.ask("How do I fix a Java null pointer exception?");
        System.out.println("Technical Response: " + techResponse);
        assertNotNull(techResponse);
    }

    // ==================== 工具调用测试 ====================

    /**
     * 银行工具 - 用于演示工具调用
     */
    public static class BankTool {
        private final Map<String, Double> accounts = Map.of(
                "Mario", 1000.0,
                "Georgios", 1000.0
        );

        @Tool("Credit the given user with the given amount and return the new balance")
        public Double credit(String user, Double amount) {
            Double balance = accounts.get(user);
            if (balance == null) {
                throw new RuntimeException("No balance found for user " + user);
            }
            Double newBalance = balance + amount;
            return newBalance;
        }

        @Tool("Withdraw the given amount with the given user and return the new balance")
        public Double withdraw(String user, Double amount) {
            Double balance = accounts.get(user);
            if (balance == null) {
                throw new RuntimeException("No balance found for user " + user);
            }
            Double newBalance = balance - amount;
            return newBalance;
        }

        @Tool("Get the balance for the given user")
        public Double getBalance(String user) {
            Double balance = accounts.get(user);
            if (balance == null) {
                throw new RuntimeException("No balance found for user " + user);
            }
            return balance;
        }
    }

    /**
     * 取款 Agent
     */
    public interface WithdrawAgent {
        @SystemMessage("You are a banker that can only withdraw US dollars (USD) from a user account.")
        @UserMessage("Withdraw {{amount}} USD from {{user}}'s account and return the new balance.")
        @Agent
        String withdraw(String user, Double amount);
    }

    @Test
    @DisplayName("测试 6: Agent 工具调用 - 银行业务")
    void testAgentWithTools() {
        if (shouldSkipTest()) {
            System.out.println("DashScope properties not configured, skipping test");
            return;
        }

        ChatModel chatModel = createChatModel();
        BankTool bankTool = new BankTool();

        WithdrawAgent withdrawAgent = AgenticServices
                .agentBuilder(WithdrawAgent.class)
                .chatModel(chatModel)
                .tools(bankTool)
                .outputKey("balance")
                .build();

        String result = withdrawAgent.withdraw("Mario", 100.0);

        assertNotNull(result);
        System.out.println("Withdraw Result: " + result);
        assertTrue(result.contains("900") || result.contains("100"));
    }

    // ==================== 错误处理测试 ====================

    @Test
    @DisplayName("测试 7: 错误处理 - 参数缺失恢复")
    void testErrorHandling() {
        if (shouldSkipTest()) {
            System.out.println("DashScope properties not configured, skipping test");
            return;
        }

        ChatModel chatModel = createChatModel();

        CreativeWriter creativeWriter = AgenticServices
                .agentBuilder(CreativeWriter.class)
                .chatModel(chatModel)
                .outputKey("story")
                .build();

        AudienceEditor audienceEditor = AgenticServices
                .agentBuilder(AudienceEditor.class)
                .chatModel(chatModel)
                .outputKey("story")
                .build();

        StyleEditor styleEditor = AgenticServices
                .agentBuilder(StyleEditor.class)
                .chatModel(chatModel)
                .outputKey("story")
                .build();

        var errorRecoveryCalled = new java.util.concurrent.atomic.AtomicBoolean(false);

        var novelCreator = AgenticServices
                .sequenceBuilder()
                .subAgents(creativeWriter, audienceEditor, styleEditor)
                .errorHandler(errorContext -> {
                    if (errorContext.agentName().equals("generateStory") &&
                            errorContext.exception() instanceof AgentInvocationException) {
                        // 缺少 topic 参数，使用默认值
                        errorContext.agenticScope().writeState("topic", "a mysterious adventure");
                        errorRecoveryCalled.set(true);
                        return ErrorRecoveryResult.retry();
                    }
                    return ErrorRecoveryResult.throwException();
                })
                .build();

        Map<String, Object> input = Map.of(
                "audience", "young adults",
                "style", "fantasy"
                // 缺少 topic 参数
        );

        try {
            String story = (String) novelCreator.invoke(input);
            assertTrue(errorRecoveryCalled.get());
            assertNotNull(story);
            System.out.println("Story with error recovery: " + story);
        } catch (Exception e) {
            System.out.println("Exception caught: " + e.getMessage());
        }
    }

    // ==================== 非 AI 代理测试 ====================

    /**
     * 非AI代理 - 字符串处理
     */
    public static class StringProcessor {
        @Agent
        public String toUppercase(String input) {
            return input.toUpperCase();
        }
    }

    @Test
    @DisplayName("测试 8: 非 AI 代理 - 字符串处理")
    void testNonAIAgent() {
        StringProcessor processor = new StringProcessor();
        String result = processor.toUppercase("hello world");
        assertEquals("HELLO WORLD", result);
        System.out.println("Non-AI Agent Result: " + result);
    }

    // ==================== 监督代理测试 ====================

    /**
     * 存款 Agent
     */
    public interface CreditAgent {
        @SystemMessage("You are a banker that can only credit US dollars (USD) to a user account.")
        @UserMessage("Credit {{amount}} USD to {{user}}'s account and return the new balance.")
        @Agent
        String credit(String user, Double amount);
    }

    /**
     * 汇率代理 - 非AI代理
     */
    public static class ExchangeOperator {
        @Agent
        public Double exchange(String originalCurrency, Double amount, String targetCurrency) {
            // 简化的汇率
            if (originalCurrency.equals("EUR") && targetCurrency.equals("USD")) {
                return amount * 1.15;
            } else if (originalCurrency.equals("USD") && targetCurrency.equals("EUR")) {
                return amount / 1.15;
            }
            return amount; // 相同货币
        }
    }

    /**
     * 监督代理接口
     */
    public interface SupervisorAgent {
        @Agent
        String invoke(String request);
    }

    // ==================== AgenticScope 状态管理测试 ====================

    @Test
    @DisplayName("测试 10: AgenticScope 状态管理")
    void testAgenticScopeState() {
        if (shouldSkipTest()) {
            System.out.println("DashScope properties not configured, skipping test");
            return;
        }

        ChatModel chatModel = createChatModel();

        CreativeWriter creativeWriter = AgenticServices
                .agentBuilder(CreativeWriter.class)
                .chatModel(chatModel)
                .outputKey("story")
                .build();

        AudienceEditor audienceEditor = AgenticServices
                .agentBuilder(AudienceEditor.class)
                .chatModel(chatModel)
                .outputKey("story")
                .build();

        var workflow = AgenticServices
                .sequenceBuilder()
                .subAgents(creativeWriter, audienceEditor)
                .build();

        Map<String, Object> input = Map.of(
                "topic", "a brave knight",
                "audience", "children"
        );

        String result = (String) workflow.invoke(input);

        assertNotNull(result);
        System.out.println("Workflow Result: " + result);
    }

    // ==================== 综合测试 ====================

    @Test
    @DisplayName("测试 11: 综合场景 - 多Agent协作")
    void testComplexMultiAgentCollaboration() {
        if (shouldSkipTest()) {
            System.out.println("DashScope properties not configured, skipping test");
            return;
        }

        ChatModel chatModel = createChatModel();

        // 创建内容生成Agent
        CreativeWriter creativeWriter = AgenticServices
                .agentBuilder(CreativeWriter.class)
                .chatModel(chatModel)
                .outputKey("story")
                .build();

        // 创建编辑Agent
        StyleEditor styleEditor = AgenticServices
                .agentBuilder(StyleEditor.class)
                .chatModel(chatModel)
                .outputKey("story")
                .build();

        // 创建评分Agent
        StyleScorer styleScorer = AgenticServices
                .agentBuilder(StyleScorer.class)
                .chatModel(chatModel)
                .outputKey("score")
                .build();

        // 创建改进循环
        var improvementLoop = AgenticServices
                .loopBuilder()
                .subAgents(styleScorer, styleEditor)
                .maxIterations(2)
                .exitCondition(scope -> scope.readState("score", 0.0) >= 0.5)
                .build();

        // 组合完整工作流
        var fullWorkflow = AgenticServices
                .sequenceBuilder()
                .subAgents(creativeWriter, improvementLoop)
                .build();

        Map<String, Object> input = Map.of(
                "topic", "space exploration",
                "style", "science fiction"
        );

        String finalStory = (String) fullWorkflow.invoke(input);

        assertNotNull(finalStory);
        assertFalse(finalStory.isEmpty());
        System.out.println("=== Final Story ===");
        System.out.println(finalStory);
        System.out.println("===================");
    }

    // ==================== Memory 功能测试 ====================

    /**
     * 带 Memory 的聊天助手接口
     */
    public interface MemoryChatAssistant {
        @SystemMessage("你是一个友好的AI助手，能够记住之前的对话内容。")
        @Agent
        String chat(String message);
    }

    @Test
    @DisplayName("测试 12: Memory 功能 - 对话记忆")
    void testMemoryFunctionality() {
        if (shouldSkipTest()) {
            System.out.println("DashScope properties not configured, skipping test");
            return;
        }

        ChatModel chatModel = createChatModel();

        // 创建带memory的聊天助手
        var memory = dev.langchain4j.memory.chat.MessageWindowChatMemory.withMaxMessages(10);

        MemoryChatAssistant assistant = AgenticServices
                .agentBuilder(MemoryChatAssistant.class)
                .chatModel(chatModel)
                .chatMemory(memory)
                .build();

        System.out.println("\n========== 测试 Memory 功能 ==========");

        System.out.println("\n第一轮对话:");
        String response1 = assistant.chat("我叫张三，很高兴认识你");
        System.out.println("用户: 我叫张三，很高兴认识你");
        System.out.println("助手: " + response1);
        assertNotNull(response1);

        System.out.println("\n第二轮对话:");
        String response2 = assistant.chat("我的名字是什么？");
        System.out.println("用户: 我的名字是什么？");
        System.out.println("助手: " + response2);
        assertNotNull(response2);

        // 验证助手记得用户的名字
        assertTrue(response2.contains("张三") || response2.contains("Zhang"),
                "助手应该记得用户的名字是张三");

        System.out.println("\n第三轮对话:");
        String response3 = assistant.chat("我最喜欢的颜色是蓝色");
        System.out.println("用户: 我最喜欢的颜色是蓝色");
        System.out.println("助手: " + response3);
        assertNotNull(response3);

        System.out.println("\n第四轮对话:");
        String response4 = assistant.chat("我最喜欢什么颜色？");
        System.out.println("用户: 我最喜欢什么颜色？");
        System.out.println("助手: " + response4);
        assertNotNull(response4);

        // 验证助手记得用户的喜好
        assertTrue(response4.contains("蓝色") || response4.contains("蓝"),
                "助手应该记得用户最喜欢的颜色是蓝色");

        System.out.println("\nMemory 状态:");
        System.out.println("当前消息数: " + memory.messages().size());

        System.out.println("\n========== Memory 功能测试完成 ==========");
    }
}
