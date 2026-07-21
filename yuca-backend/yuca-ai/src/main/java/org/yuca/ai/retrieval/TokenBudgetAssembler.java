package org.yuca.ai.retrieval;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 按 token 预算拼装 RAG 上下文，缓解 LLM "Lost in the middle" 问题。
 *
 * <p>简历项目策略："针对 LLM 'Lost in the middle' 现象设计多级 Token 预算，
 * 单节点超长按 '摘要→开头→结尾→中间截断' 优先级保留，按 token 而非数量控制"。
 *
 * <p><b>Tokenizer</b>：项目当前未引入精确 tokenizer，按字符数估算。
 * 中文 1 字 ≈ 1.5 token，英文 1 字 ≈ 0.25 token，默认预算 3000 字符 ≈ 2000-4500 token。
 * 精确 tokenizer（如 jtokkit）留给后续。
 *
 * <p><b>多级降级</b>（按优先级）：
 * <ol>
 *   <li>完整 content 能装下 → 直接用 content</li>
 *   <li>装不下但 summary 能装下 → {@code [摘要] } + summary</li>
 *   <li>都装不下且预算 ≥ 头+尾+分隔 → 头 500 + {@code [省略中段]} + 尾 500</li>
 *   <li>极小预算 → content.substring(0, budget) + {@code ...}</li>
 * </ol>
 *
 * <p>chunks 顺序消费预算，耗尽即停止——前面的（高分）节点优先占预算。
 */
@Slf4j
@Component
public class TokenBudgetAssembler {

    /** 默认字符预算（≈ 2000-4500 token 视中英文混合比） */
    public static final int DEFAULT_CHAR_BUDGET = 3000;

    /** 头尾保留字符数（头尾降级时各保留这么多） */
    private static final int HEAD_CHARS = 500;
    private static final int TAIL_CHARS = 500;

    private static final String OMITTED_MARKER = "\n...[省略中段]...\n";
    private static final String SUMMARY_PREFIX = "[摘要] ";
    private static final String TAIL_OMIT_SUFFIX = "...";

    /** RAG 上下文的固定引导语（拼装结果的开头） */
    private static final String PREAMBLE =
            "以下是从知识库中检索到的参考资料，请基于这些内容回答用户的问题。" +
                    "如果参考资料中没有相关信息，请根据你的知识回答，并说明这不是来自知识库的内容。\n\n";

    /**
     * 用默认预算拼装。
     */
    public String assemble(List<RetrievedChunk> chunks) {
        return assemble(chunks, DEFAULT_CHAR_BUDGET);
    }

    /**
     * 按指定字符预算拼装 chunks 为 RAG 上下文。
     *
     * @param chunks     已按 score 降序排好的检索结果
     * @param charBudget 字符预算上限（不含 PREAMBLE）
     * @return 拼装好的上下文字符串
     */
    public String assemble(List<RetrievedChunk> chunks, int charBudget) {
        if (chunks == null || chunks.isEmpty() || charBudget <= 0) {
            return PREAMBLE;
        }

        StringBuilder sb = new StringBuilder(PREAMBLE.length() + charBudget + 32);
        sb.append(PREAMBLE);

        int remaining = charBudget;
        int idx = 0;
        int included = 0;

        for (RetrievedChunk chunk : chunks) {
            if (remaining <= 0) {
                break;
            }
            String content = chunk.getContent();
            if (content == null || content.isEmpty()) {
                continue;
            }

            idx++;
            String header = "【参考资料 " + idx + "】\n";
            int headerCost = header.length() + 2;  // +2 是末尾 "\n\n"

            String body = fitChunk(content, chunk.getSummary(), remaining - headerCost);
            if (body == null || body.isEmpty()) {
                // 预算太小连 header 都装不下，停止
                break;
            }

            sb.append(header).append(body).append("\n\n");
            remaining -= headerCost + body.length();
            included++;
        }

        log.debug("Token 预算拼装完成: total={}, included={}, budget={}, used={}",
                chunks.size(), included, charBudget, charBudget - remaining);
        return sb.toString();
    }

    /**
     * 单节点多级降级适配。
     *
     * @return 适配后的 body；null 表示预算耗尽或无内容
     */
    private String fitChunk(String content, String summary, int budget) {
        if (budget <= 0) {
            return null;
        }

        // 1. 完整装下
        if (content.length() <= budget) {
            return content;
        }

        // 2. 摘要降级
        if (summary != null && !summary.isEmpty()) {
            String candidate = SUMMARY_PREFIX + summary;
            if (candidate.length() <= budget) {
                return candidate;
            }
        }

        // 3. 头尾降级
        int headTailCost = HEAD_CHARS + OMITTED_MARKER.length() + TAIL_CHARS;
        if (budget >= headTailCost) {
            return content.substring(0, HEAD_CHARS)
                    + OMITTED_MARKER
                    + content.substring(content.length() - TAIL_CHARS);
        }

        // 4. 仅开头（极小预算）
        if (budget > SUMMARY_PREFIX.length() + TAIL_OMIT_SUFFIX.length()) {
            int keep = budget - TAIL_OMIT_SUFFIX.length();
            return content.substring(0, keep) + TAIL_OMIT_SUFFIX;
        }

        return null;
    }
}
