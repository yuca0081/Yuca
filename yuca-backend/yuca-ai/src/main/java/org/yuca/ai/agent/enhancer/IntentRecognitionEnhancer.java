package org.yuca.ai.agent.enhancer;

import lombok.extern.slf4j.Slf4j;
import org.yuca.ai.agent.ChatContext;
import org.yuca.ai.agent.Intent;
import org.yuca.ai.core.message.ChatMessage;
import org.yuca.ai.core.message.UserMessage;
import org.yuca.ai.core.model.ChatModel;
import org.yuca.ai.core.model.ChatRequest;
import org.yuca.ai.core.model.ChatResponse;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 意图识别增强器。
 *
 * <p>在 enhancer 链最前面（{@link #order()} 返回 -1）跑，用小模型 LLM 把 query 分类为
 * chitchat / qa / task / creation 之一，写入 {@link ChatContext#setIntent(Intent)}。
 * 下游 {@link RagEnhancer} 读这个字段做路由——非 QA 时跳过 RAG 省 embedding+rerank 成本。
 *
 * <p>失败降级：LLM 调用失败 / JSON 解析失败 → 写 {@link Intent#UNKNOWN}。
 * RagEnhancer 把 UNKNOWN 当作"没有信号"处理，照常执行 RAG（保守默认，宁多调一次 API 也别漏答）。
 */
@Slf4j
public class IntentRecognitionEnhancer implements ChatEnhancer {

    private final ChatModel intentModel;

    /**
     * 宽松匹配模型输出的 intent JSON：
     * 兼容纯 JSON、被 ```json``` 包裹、前后多余解释文本等情况，取第一个含 intent 字段的对象。
     */
    private static final Pattern INTENT_JSON = Pattern.compile(
            "\\{[^{}]*\"intent\"\\s*:\\s*\"(\\w+)\"[^{}]*\\}");

    public IntentRecognitionEnhancer(ChatModel intentModel) {
        this.intentModel = intentModel;
    }

    @Override
    public int order() {
        return -1;
    }

    @Override
    public ChatRequest before(ChatRequest request, ChatContext context) {
        String query = extractLastUserMessage(request.messages());
        if (query == null || query.isBlank()) {
            context.setIntent(Intent.UNKNOWN);
            return request;
        }

        try {
            Intent intent = classify(query);
            context.setIntent(intent);
            log.debug("意图识别: query='{}', intent={}", query, intent);
        } catch (Exception e) {
            // API 异常 / 超时 / 空响应——降级为 UNKNOWN，主对话继续
            log.warn("意图识别失败，降级为 UNKNOWN: query='{}', err={}", query, e.getMessage());
            context.setIntent(Intent.UNKNOWN);
        }
        return request;
    }

    @Override
    public void after(ChatResponse response, ChatContext context) {
        // 无副作用
    }

    private Intent classify(String query) {
        String prompt = buildPrompt(query);
        ChatRequest req = ChatRequest.builder()
                .messages(List.of(UserMessage.from(prompt)))
                .build();
        ChatResponse resp = intentModel.chat(req);
        String text = resp.aiMessage() == null ? null : resp.aiMessage().text();
        return parseIntent(text);
    }

    private Intent parseIntent(String text) {
        if (text == null || text.isBlank()) {
            return Intent.UNKNOWN;
        }
        Matcher m = INTENT_JSON.matcher(text);
        if (m.find()) {
            String value = m.group(1).toLowerCase();
            return switch (value) {
                case "chitchat" -> Intent.CHITCHAT;
                case "qa" -> Intent.QA;
                case "task" -> Intent.TASK;
                case "creation" -> Intent.CREATION;
                default -> Intent.UNKNOWN;
            };
        }
        log.warn("意图 JSON 解析失败，原文: {}", text);
        return Intent.UNKNOWN;
    }

    private String buildPrompt(String query) {
        return """
                你是意图识别器。请将用户输入分类为以下之一：

                - chitchat: 闲聊、问候、感谢 (如"你好"、"谢谢")
                - qa: 知识问答、概念解释、事实查询 (如"RAG 是什么"、"为什么天是蓝的")
                - task: 需要工具执行的任务 (如"计算 2+2"、"查北京天气")
                - creation: 内容创作、写作、翻译 (如"写一篇文章"、"翻译这段话")

                只输出 JSON，不要任何其他内容：
                {"intent": "chitchat|qa|task|creation"}

                用户输入: %s
                """.formatted(query);
    }

    private String extractLastUserMessage(List<ChatMessage> messages) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            if (messages.get(i) instanceof UserMessage userMsg) {
                return userMsg.singleText();
            }
        }
        return null;
    }
}
