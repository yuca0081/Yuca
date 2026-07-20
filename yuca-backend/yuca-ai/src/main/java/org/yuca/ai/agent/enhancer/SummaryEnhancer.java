package org.yuca.ai.agent.enhancer;

import lombok.extern.slf4j.Slf4j;
import org.yuca.ai.agent.ChatContext;
import org.yuca.ai.config.AiProperties;
import org.yuca.ai.core.message.AiMessage;
import org.yuca.ai.core.message.ChatMessage;
import org.yuca.ai.core.message.SystemMessage;
import org.yuca.ai.core.message.UserMessage;
import org.yuca.ai.core.model.ChatModel;
import org.yuca.ai.core.model.ChatRequest;
import org.yuca.ai.core.model.ChatResponse;
import org.yuca.ai.history.ChatHistory;
import org.yuca.ai.history.ChatHistoryStore;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 对话历史摘要压缩增强器。
 *
 * <p>替代 {@code HistoryEnhancer} 的固定窗口截断——固定截断会把早期决策原文整段丢弃，
 * 多轮协作场景下用户常因此感觉 AI "失忆"。本增强器在历史超过阈值时调用小模型把最早一批
 * 消息压缩为单段 SUMMARY 持久化到 ai_chat_history，下次 {@link ChatHistoryStore#getActiveMessages}
 * 只返回 [SUMMARY, ...最近 raw 消息]，既省 token 又保留长程语义。
 *
 * <p>order=-2，比 {@code IntentRecognitionEnhancer}(-1) 和 {@code HistoryEnhancer}(0) 都早，
 * 确保本轮摘要写入后 HistoryEnhancer 立刻能看到新的活跃边界。
 *
 * <p>滚动摘要：触发时被摘要的范围含上一条 SUMMARY + 其后的 raw 消息，新 SUMMARY 自然取代旧的。
 * 旧 SUMMARY 与被取代的 raw 消息仍留在 DB（审计/调试用），只是 getActiveMessages 不再返回。
 *
 * <p>失败降级：摘要 LLM 调用失败 / 返回空 → 跳过本轮摘要，HistoryEnhancer 继续走原固定窗口逻辑，
 * 不影响主对话。
 */
@Slf4j
public class SummaryEnhancer implements ChatEnhancer {

    private final ChatHistoryStore historyStore;
    private final AiProperties.SummaryConfig config;
    private final ChatModel summaryModel;

    public SummaryEnhancer(ChatHistoryStore historyStore,
                           AiProperties.SummaryConfig config,
                           ChatModel summaryModel) {
        this.historyStore = historyStore;
        this.config = config;
        this.summaryModel = summaryModel;
    }

    @Override
    public int order() {
        return -2;
    }

    @Override
    public ChatRequest before(ChatRequest request, ChatContext context) {
        if (!config.isEnabled()) {
            return request;
        }
        String sessionId = context.getSessionId();
        if (sessionId == null) {
            return request;
        }

        try {
            List<ChatMessage> active = historyStore.getActiveMessages(sessionId);
            if (active.size() <= config.getThreshold()) {
                return request;
            }

            int summarizeCount = active.size() - config.getKeepRecent();
            if (summarizeCount <= 0) {
                return request;
            }
            List<ChatMessage> toSummarize = active.subList(0, summarizeCount);

            String summary = summarize(toSummarize);
            if (summary == null || summary.isBlank()) {
                log.warn("摘要 LLM 返回空，跳过本次摘要: sessionId={}", sessionId);
                return request;
            }

            // 直接构造 ChatHistory 实体插入，绕过 toChatHistory 的 USER/AI 类型分支
            ChatHistory summaryHistory = new ChatHistory();
            summaryHistory.setSessionId(sessionId);
            summaryHistory.setMessageType("SUMMARY");
            summaryHistory.setContent(summary.trim());
            summaryHistory.setCreatedAt(LocalDateTime.now());
            historyStore.appendMessages(sessionId, List.of(summaryHistory));

            log.info("历史摘要完成: sessionId={}, 压缩 {} 条 → {} 字符, 保留最近 {} 条原文",
                    sessionId, summarizeCount, summary.length(), config.getKeepRecent());
        } catch (Exception e) {
            // 任何异常都不阻断主对话：摘要失败最多这一轮上下文长一点，没有副作用
            log.warn("历史摘要失败，降级使用未摘要历史: sessionId={}, err={}",
                    sessionId, e.getMessage());
        }
        return request;
    }

    @Override
    public void after(ChatResponse response, ChatContext context) {
        // 摘要是读侧逻辑：写入由 HistoryEnhancer.after 负责
    }

    /**
     * 调小模型压缩历史消息为单段摘要
     */
    private String summarize(List<ChatMessage> messages) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请把以下对话历史压缩为一段简洁的摘要，保留关键事实、用户偏好、未完成任务和重要决策，");
        prompt.append("丢弃寒暄与冗余细节。直接输出摘要正文，不要任何前缀或解释：\n\n");

        for (ChatMessage msg : messages) {
            prompt.append("- ").append(renderRole(msg)).append(": ")
                    .append(renderText(msg)).append("\n");
        }

        ChatRequest req = ChatRequest.builder()
                .messages(List.of(UserMessage.from(prompt.toString())))
                .build();
        ChatResponse resp = summaryModel.chat(req);
        return resp.aiMessage() == null ? null : resp.aiMessage().text();
    }

    private String renderRole(ChatMessage msg) {
        if (msg instanceof UserMessage) return "用户";
        if (msg instanceof AiMessage) return "助手";
        if (msg instanceof SystemMessage) return "前期摘要";
        return msg.type().toString();
    }

    private String renderText(ChatMessage msg) {
        if (msg instanceof UserMessage u) return u.singleText();
        if (msg instanceof AiMessage a) return a.text() != null ? a.text() : "[工具调用]";
        if (msg instanceof SystemMessage s) return s.text();
        return "";
    }
}
