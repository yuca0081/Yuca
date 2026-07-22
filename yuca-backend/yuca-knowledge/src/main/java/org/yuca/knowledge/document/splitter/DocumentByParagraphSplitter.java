package org.yuca.knowledge.document.splitter;

import org.yuca.knowledge.document.ChapterNode;
import org.yuca.knowledge.document.DocumentByCharacterSplitter;

import java.util.ArrayList;
import java.util.List;

/**
 * 段落切片器（#11 文档质量路由：Clean-非md / Decent 路径用）。
 *
 * <p>策略：
 * <ol>
 *   <li>按双换行（{@code \n\s*\n}）切段落</li>
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
