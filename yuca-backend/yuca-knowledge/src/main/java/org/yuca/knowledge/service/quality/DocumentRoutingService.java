package org.yuca.knowledge.service.quality;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.yuca.knowledge.document.ChapterNode;
import org.yuca.knowledge.document.DocumentByCharacterSplitter;
import org.yuca.knowledge.document.Document;
import org.yuca.knowledge.document.MarkdownChapterTreeBuilder;
import org.yuca.knowledge.document.splitter.DocumentByParagraphSplitter;

import java.util.ArrayList;
import java.util.List;

/**
 * 文档切片策略路由（#11：质量评分与智能路由）。
 *
 * <p>基于 {@link QualityScore#getTier()} 选切片策略：
 * <ul>
 *   <li><b>Clean</b>：md + 有标题 → 章节树；非 md → 段落切片</li>
 *   <li><b>Decent</b>：段落切片（语义边界优于固定字符）</li>
 *   <li><b>Garbage</b>：字符切片兜底 + WARN 日志（#12 OCR 来时把这里改成调 OcrParser 后重新评分）</li>
 * </ul>
 *
 * <p>开关：{@code yuca.knowledge.quality.enabled=false} 时走原硬编码路径
 * （md + 有标题 → 章节树，否则字符切片），行为等价于重构前。
 *
 * @author Yuca
 * @since 2026-07-22
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentRoutingService {

    private final MarkdownChapterTreeBuilder markdownChapterTreeBuilder;
    private final DocumentByParagraphSplitter paragraphSplitter;

    @Value("${yuca.knowledge.quality.enabled:true}")
    private boolean enabled;

    /** 扁平字符切片参数：与重构前 KnowledgeDocService.splitFlat 保持一致（100 字符 + 10 重叠） */
    private static final int FLAT_CHUNK_SIZE = 100;
    private static final int FLAT_CHUNK_OVERLAP = 10;

    /**
     * 路由入口：根据开关 + 评分选切片策略。
     *
     * @param content    解析后的纯文本
     * @param fileFormat 文件扩展名（小写）
     * @param score      质量评分（{@code enabled=false} 时可传 null）
     * @return 章节树根节点列表（headingLevel=0 表示扁平切片）
     */
    public List<ChapterNode> route(String content, String fileFormat, QualityScore score) {
        if (!enabled || score == null) {
            // 开关关闭：走原硬编码路径，行为等价于重构前
            return routeLegacy(content, fileFormat);
        }

        return switch (score.getTier()) {
            case QualityScore.TIER_CLEAN -> routeClean(content, fileFormat);
            case QualityScore.TIER_DECENT -> {
                log.info("[route] 走段落切片: overall={}, textRatio={}",
                        score.getOverall(), score.getTextRatio());
                yield paragraphSplitter.split(content);
            }
            case QualityScore.TIER_GARBAGE -> {
                log.warn("[route] 文档质量差，走字符切片兜底: overall={}, textRatio={}, noise={}, reason={}",
                        score.getOverall(), score.getTextRatio(),
                        score.getOcrNoiseDensity(), score.getReason());
                yield splitFlat(content);
            }
            default -> {
                log.warn("[route] 未知 tier，降级字符切片: tier={}", score.getTier());
                yield splitFlat(content);
            }
        };
    }

    /**
     * Clean 路由：md + 有标题 → 章节树；否则段落切片。
     */
    private List<ChapterNode> routeClean(String content, String fileFormat) {
        if ("md".equalsIgnoreCase(fileFormat) && markdownChapterTreeBuilder.hasHeadings(content)) {
            List<ChapterNode> roots = markdownChapterTreeBuilder.build(content);
            log.info("[route] 走章节树切片: fileFormat=md, roots={}", roots.size());
            return roots;
        }
        log.info("[route] 走段落切片: fileFormat={}", fileFormat);
        return paragraphSplitter.split(content);
    }

    /**
     * 旧硬编码路径（开关关闭时用）：md + 有标题 → 章节树；否则字符切片。
     */
    private List<ChapterNode> routeLegacy(String content, String fileFormat) {
        if ("md".equalsIgnoreCase(fileFormat) && markdownChapterTreeBuilder.hasHeadings(content)) {
            return markdownChapterTreeBuilder.build(content);
        }
        return splitFlat(content);
    }

    /**
     * 扁平字符切片（与重构前 KnowledgeDocService.splitFlat 同构）。
     * 输出 headingLevel=0 的 ChapterNode，后续 saveChapterNodes 走扁平路径。
     */
    private List<ChapterNode> splitFlat(String content) {
        DocumentByCharacterSplitter splitter = new DocumentByCharacterSplitter(FLAT_CHUNK_SIZE, FLAT_CHUNK_OVERLAP);
        List<String> chunks = splitter.split(Document.from(content));
        List<ChapterNode> roots = new ArrayList<>(chunks.size());
        for (String chunk : chunks) {
            ChapterNode node = new ChapterNode();
            node.setHeadingLevel(0);
            node.setContent(chunk);
            roots.add(node);
        }
        return roots;
    }
}
