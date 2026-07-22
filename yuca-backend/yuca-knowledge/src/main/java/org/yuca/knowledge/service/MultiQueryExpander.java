package org.yuca.knowledge.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.yuca.ai.config.AiProperties;
import org.yuca.ai.core.ChatModelFactory;
import org.yuca.ai.core.message.UserMessage;
import org.yuca.ai.core.model.ChatModel;
import org.yuca.ai.core.model.ChatRequest;
import org.yuca.ai.core.model.ChatResponse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 多查询扩展服务（#9：Multi-Query Expansion）。
 *
 * <p>调 LLM 把用户原始 query 改写为 N 个不同视角的子 query（强调"视角不同"，
 * 避免退化成 #8 的同义词替换）。返回的列表<b>不含原 query</b>——调用方需手动把原 query
 * 拼到第一位，便于复用其 embedding。
 *
 * <p>与 {@link QueryExpansionService}（#8）的关系：两者独立开关、可叠加。
 * 典型组合：{@code RetrievalService} 循环每个子 query 喂 BM25 时，每个都先过 #8 扩展。
 *
 * <p>失败降级：LLM 调用或输出解析失败时返回 {@link Collections#emptyList()}，
 * 调用方仅用原 query 单路召回，检索流程不阻塞。
 *
 * <p>依赖说明：本类只做单轮 LLM 调用（无历史 / 工具 / 增强器），故直接用
 * {@link ChatModelFactory} 构造 {@link ChatModel}，不依赖 {@code AgentFactory}，
 * 避免 AgentFactory ↔ RetrievalService ↔ MultiQueryExpander 循环依赖。
 *
 * @author Yuca
 * @since 2026-07-22
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MultiQueryExpander {

    private final ChatModelFactory chatModelFactory;
    private final AiProperties aiProperties;

    /** 改写数量。简历说 3-5 个，取下限避免延迟与噪声稀释 */
    private static final int REWRITE_COUNT = 3;
    /** 单条改写最大长度（字符）。超长视为 LLM 跑偏，过滤掉 */
    private static final int MAX_REWRITE_LEN = 200;

    @Value("${yuca.knowledge.multi-query.enabled:true}")
    private boolean enabled;

    /**
     * 改写原 query 为 N 个不同视角的子 query。
     *
     * @param query 用户原始 query
     * @return 改写列表（不含原 query）；未启用或异常时返回空列表
     */
    public List<String> rewrite(String query) {
        if (!enabled) {
            return Collections.emptyList();
        }
        if (query == null || query.isBlank()) {
            return Collections.emptyList();
        }

        try {
            String prompt = buildPrompt(query);
            ChatRequest req = ChatRequest.builder()
                    .messages(List.of(UserMessage.from(prompt)))
                    .build();
            ChatModel chatModel = chatModelFactory.chatModel(aiProperties.getSummary().getModelName());
            ChatResponse resp = chatModel.chat(req);

            if (resp.aiMessage() == null || resp.aiMessage().text() == null) {
                log.debug("[multiQuery] LLM 返回空，降级单 query: query='{}'", query);
                return Collections.emptyList();
            }

            List<String> rewrites = parseRewrites(resp.aiMessage().text(), query);
            log.debug("[multiQuery] 改写成功: original='{}', rewrites={}", query, rewrites);
            return rewrites;
        } catch (Exception e) {
            log.warn("[multiQuery] 改写失败，降级单 query: query='{}', error={}",
                    query, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 解析 LLM 输出为改写列表。
     *
     * <p>规则：按行切分 → trim → 过滤空 / 超长 / 含序号前缀 / 与原 query 等值 → 去重 → limit N。
     * 用 {@link LinkedHashSet} 去重保留首次出现顺序，保证 RRF 跨 query 加分稳定。
     */
    private List<String> parseRewrites(String output, String originalQuery) {
        Set<String> seen = new LinkedHashSet<>();
        String trimmedOriginal = originalQuery.trim();
        for (String rawLine : output.split("\n")) {
            String line = rawLine.trim();
            if (line.isEmpty()) {
                continue;
            }
            // 去掉 LLM 常见的序号前缀："1. " / "1、 " / "- " / "* "
            line = stripOrderPrefix(line);
            // 长度过滤：太短无语义，太长 LLM 跑偏
            if (line.length() < 2 || line.length() > MAX_REWRITE_LEN) {
                continue;
            }
            // 与原 query 等值（大小写不敏感）则跳过——调用方已把原 query 放在第一位
            if (line.equalsIgnoreCase(trimmedOriginal)) {
                continue;
            }
            seen.add(line);
            if (seen.size() >= REWRITE_COUNT) {
                break;
            }
        }
        return new ArrayList<>(seen);
    }

    /** 去掉行首的序号前缀：纯数字+点/顿号，或 markdown 列表符号 */
    private String stripOrderPrefix(String line) {
        if (line.isEmpty()) {
            return line;
        }
        // 形如 "1. " / "12、 " / "1）"
        int i = 0;
        while (i < line.length() && Character.isDigit(line.charAt(i))) {
            i++;
        }
        if (i > 0 && i < line.length()) {
            char sep = line.charAt(i);
            if (sep == '.' || sep == '、' || sep == '）' || sep == ')') {
                return line.substring(i + 1).trim();
            }
        }
        // markdown 列表
        if (line.startsWith("- ") || line.startsWith("* ")) {
            return line.substring(2).trim();
        }
        return line;
    }

    private String buildPrompt(String query) {
        return """
                请把下面的用户问题改写为 %d 个不同视角但语义等价的问题，用于知识库检索的多查询融合。
                要求：
                - 每行一个问题
                - 用不同的表达方式、术语或视角（不要简单同义词替换）
                - 保持原意，不要扩展或缩小范围
                - 不要加序号、引号、Markdown 标记

                原始问题：
                %s
                """.formatted(REWRITE_COUNT, query);
    }
}
