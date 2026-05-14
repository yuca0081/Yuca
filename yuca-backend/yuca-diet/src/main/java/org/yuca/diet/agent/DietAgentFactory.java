package org.yuca.diet.agent;

import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.model.chat.ChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.yuca.ai.agent.Agent;
import org.yuca.ai.agent.ChatContext;
import org.yuca.ai.agent.enhancer.HistoryEnhancer;
import org.yuca.ai.agent.enhancer.SystemPromptEnhancer;
import org.yuca.ai.config.AiProperties;
import org.yuca.ai.history.ChatHistoryStore;
import org.yuca.ai.tool.Calculator;
import org.yuca.ai.tool.ToolExtractor;
import org.yuca.diet.tool.DietTool;

import java.util.List;

/**
 * 饮食 Agent 工厂
 * 在 diet 模块内组装专用的饮食 Agent
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DietAgentFactory {

    private final AiProperties aiProperties;
    private final ChatHistoryStore historyStore;
    private final DietTool dietTool;

    /**
     * 创建饮食 Agent
     */
    public Agent createDietAgent(ChatContext context) {
        String systemPrompt = buildDietSystemPrompt();

        var enhancers = List.of(
                new SystemPromptEnhancer(systemPrompt),
                new HistoryEnhancer(historyStore, 50)
        );

        List<Object> toolObjects = List.of(dietTool, new Calculator());
        ToolExtractor toolExtractor = new ToolExtractor(toolObjects);

        return Agent.builder()
                .chatModel(buildChatModel())
                .context(context)
                .enhancers(enhancers)
                .toolSpecifications(toolExtractor.getSpecifications())
                .toolExecutors(toolExtractor.getExecutors())
                .build();
    }

    private ChatModel buildChatModel() {
        AiProperties.ProviderConfig dashscope = aiProperties.getDashscope();
        return QwenChatModel.builder()
                .modelName(dashscope.getModelName())
                .apiKey(dashscope.getApiKey())
                .build();
    }

    private String buildDietSystemPrompt() {
        return """
                你是一位专业的饮食管理助手，专门帮助用户管理饮食健康。

                ## 你的能力
                1. 记录用户的饮食（通过 addDietRecord 工具）
                2. 查询用户的饮食记录（通过 queryDietRecords 工具）
                3. 获取每日营养汇总（通过 getDailyNutritionSummary 工具）
                4. 查看用户的饮食目标（通过 getDietGoal 工具）

                ## 工作方式
                - 当用户描述吃了什么食物时，主动提取食物名称、数量，估算热量和营养素，然后调用 addDietRecord 记录
                - 当用户询问饮食情况时，先查询记录再给出分析建议
                - 餐次类型：1=早餐, 2=午餐, 3=晚餐, 4=加餐
                - 日期格式：YYYY-MM-DD
                - 如果用户没有指定日期，默认使用今天（%s）
                - 所有热量单位为千卡(kcal)，重量单位默认为克(g)

                ## 注意
                - 估算食物热量时参考上述数据，给出合理估值
                - 如果用户一次说了多种食物，分别记录每一条
                - 回答要友好、专业，适当给出营养建议
                - 如果用户和你的聊天脱离了饮食相关的主题，则提醒用户你的身份是饮食管理助手，只能聊饮食相关的话题
                """.formatted(java.time.LocalDate.now());
    }
}
