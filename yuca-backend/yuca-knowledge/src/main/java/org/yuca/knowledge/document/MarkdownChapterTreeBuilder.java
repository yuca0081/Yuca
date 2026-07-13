package org.yuca.knowledge.document;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 基于 Markdown H1-H6 标题层级的章节树构造器。
 *
 * <p>核心算法：
 * <ol>
 *   <li>逐行扫描，正则识别标题行，维护 inCodeFence 状态避免误判代码块内的 {@code #} 注释</li>
 *   <li>独占式切片：每节点 content = 自己标题行后到下一任意级别标题行前的内容</li>
 *   <li>栈算法建父子关系：新节点弹出栈顶所有 level >= 自己的节点，新节点是栈顶的孩子</li>
 *   <li>面包屑：根节点 = title；非根 = parent.breadcrumb + " > " + title</li>
 * </ol>
 *
 * <p>文章《1500 行代码，召回率翻 3.4 倍》："用栈算法建父子关系——遇到 H2 弹出栈顶的 H3-H6，
 * 新节点总是栈顶的孩子"。
 */
@Slf4j
public class MarkdownChapterTreeBuilder {

    /** Markdown 标题行：1-6 个 # + 至少一个空格 + 标题文本，末尾 # 可选 */
    private static final Pattern HEADING = Pattern.compile("^(#{1,6})\\s+(.+?)\\s*#*$");

    /** 代码块围栏：单独成行的 ``` 或 ~~~（可带语言标识如 ```java） */
    private static final Pattern CODE_FENCE = Pattern.compile("^(```|~~~)(.*)?$");

    /**
     * 解析 markdown 文本为章节树根节点列表。
     *
     * <p>通常返回 1 个根（文档以 H1 开头）。如果文档开头有无标题的前导内容，
     * 会额外产生一个 level=1、title="简介" 的虚拟根节点。
     *
     * @return 根节点列表；无标题时返回空列表（调用方应降级到字符切片）
     */
    public List<ChapterNode> build(String markdown) {
        if (markdown == null || markdown.isEmpty()) {
            return List.of();
        }
        // 用 -1 保留末尾空行，保证行号语义正确
        String[] lines = markdown.split("\n", -1);

        // === Pass 1: 识别所有标题行（跳过代码块内） ===
        List<HeadingMatch> headings = scanHeadings(lines);
        if (headings.isEmpty()) {
            return List.of();
        }

        // === 构造所有节点（含前导内容的"简介"虚拟节点） ===
        List<ChapterNode> nodes = new ArrayList<>();

        // 前导内容：第一个标题行之前如果有非空内容，作为虚拟"简介"节点
        int firstHeadingLine = headings.get(0).lineIndex;
        if (firstHeadingLine > 0) {
            String preamble = joinLines(lines, 0, firstHeadingLine - 1).trim();
            if (!preamble.isEmpty()) {
                ChapterNode intro = new ChapterNode();
                intro.setTitle("简介");
                intro.setHeadingLevel(1);
                intro.setContent(preamble);
                intro.setLineStart(0);
                intro.setLineEnd(firstHeadingLine - 1);
                nodes.add(intro);
            }
        }

        // 各标题节点：独占式切片，行号 = [标题行, 下一标题行 - 1]
        for (int i = 0; i < headings.size(); i++) {
            HeadingMatch h = headings.get(i);
            int nextHeadingLine = (i + 1 < headings.size())
                    ? headings.get(i + 1).lineIndex
                    : lines.length;

            ChapterNode node = new ChapterNode();
            node.setTitle(h.title);
            node.setHeadingLevel(h.level);
            node.setLineStart(h.lineIndex);
            node.setLineEnd(nextHeadingLine - 1);
            // 独占式切片：标题行之后到下一标题行之前
            String content = (h.lineIndex + 1 <= nextHeadingLine - 1)
                    ? joinLines(lines, h.lineIndex + 1, nextHeadingLine - 1).trim()
                    : "";
            node.setContent(content);
            nodes.add(node);
        }

        // === Pass 2: 栈算法建父子关系 + 面包屑 ===
        List<ChapterNode> roots = new ArrayList<>();
        Deque<ChapterNode> stack = new ArrayDeque<>();

        for (ChapterNode node : nodes) {
            // 弹出栈顶所有 level >= 当前 node 的（同层兄弟、更深的子节点）
            while (!stack.isEmpty() && stack.peek().getHeadingLevel() >= node.getHeadingLevel()) {
                stack.pop();
            }
            if (stack.isEmpty()) {
                node.setBreadcrumb(node.getTitle());
                roots.add(node);
            } else {
                ChapterNode parent = stack.peek();
                node.setBreadcrumb(parent.getBreadcrumb() + " > " + node.getTitle());
                parent.addChild(node);
            }
            stack.push(node);
        }

        log.debug("MarkdownChapterTreeBuilder: 共 {} 个标题 → {} 个根节点，总节点数 {}",
                headings.size(), roots.size(), nodes.size());
        return roots;
    }

    /**
     * 快速检测 markdown 是否包含至少一个标题（不构造树）。
     * 调用方据此决定走章节树路径还是降级到字符切片。
     */
    public boolean hasHeadings(String markdown) {
        if (markdown == null || markdown.isEmpty()) {
            return false;
        }
        return !scanHeadings(markdown.split("\n", -1)).isEmpty();
    }

    /** 扫描所有行，返回按文档顺序排列的标题（已过滤代码块内的误判） */
    private List<HeadingMatch> scanHeadings(String[] lines) {
        List<HeadingMatch> headings = new ArrayList<>();
        boolean inCodeFence = false;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (CODE_FENCE.matcher(line).matches()) {
                inCodeFence = !inCodeFence;
                continue;
            }
            if (inCodeFence) {
                continue;
            }
            Matcher m = HEADING.matcher(line);
            if (m.matches()) {
                headings.add(new HeadingMatch(i, m.group(1).length(), m.group(2).trim()));
            }
        }
        return headings;
    }

    /** 拼接 lines[start..end]（含两端），用 \n 连接 */
    private String joinLines(String[] lines, int start, int end) {
        if (start > end) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = start; i <= end; i++) {
            if (i > start) {
                sb.append("\n");
            }
            sb.append(lines[i]);
        }
        return sb.toString();
    }

    private record HeadingMatch(int lineIndex, int level, String title) {}
}
