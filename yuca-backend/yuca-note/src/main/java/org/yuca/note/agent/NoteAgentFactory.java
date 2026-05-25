package org.yuca.note.agent;

import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.yuca.ai.agent.Agent;
import org.yuca.ai.agent.ChatContext;
import org.yuca.ai.agent.enhancer.HistoryEnhancer;
import org.yuca.ai.agent.enhancer.SystemPromptEnhancer;
import org.yuca.ai.config.AiProperties;
import org.yuca.ai.history.ChatHistoryStore;
import org.yuca.ai.tool.ToolExtractor;
import org.yuca.note.tool.NoteTool;

import java.util.List;

/**
 * 笔记 Agent 工厂
 * 在 note 模块内组装专用的笔记 Agent
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NoteAgentFactory {

    private final AiProperties aiProperties;
    private final ChatHistoryStore historyStore;
    private final NoteTool noteTool;

    /**
     * 创建笔记 Agent（通用对话，带工具和历史）
     */
    public Agent createNoteAgent(ChatContext context) {
        String systemPrompt = buildNoteSystemPrompt();

        var enhancers = List.of(
                new SystemPromptEnhancer(systemPrompt),
                new HistoryEnhancer(historyStore, 50)
        );

        List<Object> toolObjects = List.of(noteTool);
        ToolExtractor toolExtractor = new ToolExtractor(toolObjects);

        return Agent.builder()
                .chatModel(buildChatModel())
                .context(context)
                .enhancers(enhancers)
                .toolSpecifications(toolExtractor.getSpecifications())
                .toolExecutors(toolExtractor.getExecutors())
                .build();
    }

    /**
     * 创建文档操作 Agent（单次操作，无历史无工具）
     */
    public Agent createDocActionAgent(String action, String title, String content, String targetLanguage) {
        String systemPrompt = buildDocActionPrompt(action, targetLanguage);
        String userMessage = "## " + title + "\n\n" + content;

        ChatContext context = new ChatContext();

        List<org.yuca.ai.agent.enhancer.ChatEnhancer> enhancers = List.of(
                new SystemPromptEnhancer(systemPrompt)
        );

        return Agent.builder()
                .chatModel(buildChatModel())
                .context(context)
                .enhancers(enhancers)
                .build();
    }

    /**
     * 构建文档操作请求
     */
    public ChatRequest buildDocActionRequest(String title, String content) {
        return ChatRequest.builder()
                .messages(List.of(new UserMessage("## " + title + "\n\n" + content)))
                .build();
    }

    private ChatModel buildChatModel() {
        AiProperties.ProviderConfig dashscope = aiProperties.getDashscope();
        return QwenChatModel.builder()
                .modelName(dashscope.getModelName())
                .apiKey(dashscope.getApiKey())
                .build();
    }

    private String buildNoteSystemPrompt() {
        return """
                你是一位专业的笔记助手，专门帮助用户管理和编辑笔记。

                ## 你的能力
                1. 搜索用户的笔记（通过 searchNotes 工具）
                2. 获取笔记的完整内容（通过 getNoteContent 工具）
                3. 列出最近编辑的笔记（通过 listRecentNotes 工具）
                4. 更新笔记内容（通过 updateNoteContent 工具）
                5. 列出用户的笔记本（通过 listNoteBooks 工具）

                ## 工作方式
                - 当用户询问笔记内容时，先搜索或获取相关笔记，再给出回答
                - 当用户要求修改笔记时，确认后通过 updateNoteContent 工具更新
                - 可以帮助用户总结、分析、整理笔记内容
                - 可以基于现有笔记内容进行续写、扩写、润色
                - 回答要友好、专业，给出有价值的建议

                ## 注意
                - 如果用户和你的聊天脱离了笔记相关的主题，则提醒用户你的身份是笔记助手，主要帮助管理笔记
                - 操作笔记前先确认用户的意图，避免误操作
                - 更新笔记内容时会完全替换原有内容，请谨慎操作
                """;
    }

    private String buildDocActionPrompt(String action, String targetLanguage) {
        return switch (action.toUpperCase()) {
            case "SUMMARIZE" -> "你是一位专业的文本编辑助手。请总结以下文档内容，生成简洁的摘要。保留关键信息和要点，使用清晰的列表格式。直接输出摘要内容，不要添加多余的前缀说明。";
            case "TRANSLATE" -> String.format("你是一位专业的翻译助手。请将以下文档内容翻译为%s。保持原文的格式和结构，翻译要准确、自然、流畅。直接输出翻译结果，不要添加多余的前缀说明。", targetLanguage != null ? targetLanguage : "英文");
            case "POLISH" -> "你是一位专业的文字润色助手。请润色以下文档内容，改善表达和文风，使其更加流畅、专业和易读。保持原文含义不变，保留原有结构。直接输出润色后的内容，不要添加多余的前缀说明。";
            case "EXPAND" -> "你是一位专业的内容扩写助手。请在保持原有结构和主题的基础上，扩展以下文档内容，增加更多细节、论据和论述。使内容更加丰富和充实。直接输出扩写后的内容，不要添加多余的前缀说明。";
            case "OUTLINE" -> "你是一位专业的内容分析助手。请为以下文档内容生成一个层次分明的提纲，使用 Markdown 格式。直接输出提纲内容，不要添加多余的前缀说明。";
            default -> "你是一位专业的文本编辑助手。请根据用户的要求处理以下文档内容。直接输出处理结果，不要添加多余的前缀说明。";
        };
    }
}
