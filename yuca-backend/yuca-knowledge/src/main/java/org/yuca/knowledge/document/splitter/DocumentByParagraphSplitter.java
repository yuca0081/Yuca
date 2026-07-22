package org.yuca.knowledge.document.splitter;

import org.yuca.knowledge.document.ChapterNode;
import org.yuca.knowledge.document.DocumentByCharacterSplitter;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 段落切片器（#11 文档质量路由：Clean-非md / Decent 路径用）。
 *
 * <p>策略：
 * <ol>
 *   <li>按双换行（{@code \n\s*\n}）切段落</li>
 *   <li>标题与正文合并：以 md 标题（{@code # 一级标题}）开头的段落与下一段合并为一片，
 *       避免非 md 路径（pdf/docx 解析后）恰好出现 {@code #} 开头的行被切断</li>
 *   <li>段落数 &lt; 3 时降级到字符切片——小文档段落语义弱，字符切片反而更均匀</li>
 *   <li>单段落长度 &gt; {@link #MAX_PARAGRAPH_LEN} 时调 {@link DocumentByCharacterSplitter} 二次切</li>
 *   <li>输出 {@code headingLevel=0} 的扁平 ChapterNode，与 {@code KnowledgeDocService.splitFlat} 同构</li>
 * </ol>
 *
 * <p>设计目标：填补"非 md 无章节树"的空白路径——之前统一走 100 字符固定切片，
 * 段落边界被切断导致语义流失；本切片器保留段落完整性，长段落再字符降级。
 *
 * @author Yuca
 * @since 2026-07-22
 */
public class DocumentByParagraphSplitter {

    /** 单段落最大长度，超出则字符切片降级。500 字符约等于 chunk embedding 的最佳信号长度 */
    private static final int MAX_PARAGRAPH_LEN = 500;
    /** 长段落字符切片重叠量 */
    private static final int PARAGRAPH_OVERLAP = 50;
    /** 段落数下限：少于此值降级字符切片 */
    private static final int MIN_PARAGRAPH_COUNT = 3;

    /** md 标题行正则：1-6 个 # + 至少一个空格 + 文本。用于"标题 + 正文"合并 */
    private static final Pattern HEADING_LINE = Pattern.compile("^#{1,6}\\s.+");

    private final DocumentByCharacterSplitter fallback = new DocumentByCharacterSplitter(MAX_PARAGRAPH_LEN, PARAGRAPH_OVERLAP);

    /**
     * 按段落切片。
     *
     * @param content 原始纯文本
     * @return ChapterNode 列表（headingLevel=0，扁平结构）
     */
    public List<ChapterNode> split(String content) {
        if (content == null || content.isBlank()) {
            return new ArrayList<>();
        }

        // 按双换行（含任意空白）切段落
        String[] rawParagraphs = content.split("\\n\\s*\\n");
        List<String> paragraphs = new ArrayList<>(rawParagraphs.length);
        for (String p : rawParagraphs) {
            String stripped = p.strip();
            if (!stripped.isEmpty()) {
                paragraphs.add(stripped);
            }
        }

        // 标题与正文合并：避免标题被切成独立片段，丢失与正文的语义关联
        paragraphs = mergeHeadingsWithBody(paragraphs);

        // 段落太少：降级字符切片，保证切片数稳定（避免一篇只有 2 段的文档切片过粗）
        if (paragraphs.size() < MIN_PARAGRAPH_COUNT) {
            return toChapterNodes(fallback.split(content));
        }

        List<String> chunks = new ArrayList<>(paragraphs.size());
        for (String p : paragraphs) {
            if (p.length() > MAX_PARAGRAPH_LEN) {
                // 长段落字符切片降级，保留语义连贯同时控制 embedding 信号长度
                chunks.addAll(fallback.split(p));
            } else {
                chunks.add(p);
            }
        }
        return toChapterNodes(chunks);
    }

    /**
     * 防御性合并：把以 md 标题（{@code #..# 文本}）开头的段落与下一个段落合并为一片。
     *
     * <p>场景：pdf/docx 解析后的纯文本如果恰好包含 {@code #} 开头的行，
     * 之前的逻辑会把"标题段"和"正文段"切成两片，导致标题脱离正文、正文失去上下文。
     * 本方法保证"标题 + 紧跟正文"至少作为一个整体入库。
     *
     * <p>边界：
     * <ul>
     *   <li>标题段是最后一段（无下一段）：保留原样，不合并</li>
     *   <li>标题段后紧跟另一个标题段：合并成一片（嵌套标题扁平化），多层嵌套场景应由 md 章节树路径处理</li>
     *   <li>合并后总长度 &gt; {@link #MAX_PARAGRAPH_LEN}：交给后续长段落降级处理</li>
     * </ul>
     */
    private List<String> mergeHeadingsWithBody(List<String> paragraphs) {
        if (paragraphs.size() <= 1) {
            return paragraphs;
        }
        List<String> merged = new ArrayList<>(paragraphs.size());
        for (int i = 0; i < paragraphs.size(); i++) {
            String p = paragraphs.get(i);
            if (HEADING_LINE.matcher(p).matches() && i + 1 < paragraphs.size()) {
                merged.add(p + "\n\n" + paragraphs.get(i + 1));
                i++;  // 跳过已合并的下一段
            } else {
                merged.add(p);
            }
        }
        return merged;
    }

    private List<ChapterNode> toChapterNodes(List<String> chunks) {
        List<ChapterNode> result = new ArrayList<>(chunks.size());
        for (String chunk : chunks) {
            if (chunk == null || chunk.isBlank()) {
                continue;
            }
            ChapterNode node = new ChapterNode();
            node.setHeadingLevel(0);
            node.setContent(chunk);
            result.add(node);
        }
        return result;
    }
}
