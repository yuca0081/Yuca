package org.yuca.knowledge.document;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 章节树节点（内存 DTO，与 DB 实体解耦）。
 *
 * <p>对应基于 H1-H6 标题层级构建的独占式章节树。每个节点携带：
 * <ul>
 *   <li>标题、层级（1-6）、面包屑路径——为 embedding 提供上下文语义</li>
 *   <li>独占式切片正文（不含子节点正文）</li>
 *   <li>源文件起止行号——便于精确溯源</li>
 *   <li>子节点列表——拓扑关系</li>
 * </ul>
 *
 * <p>独占式切片：父节点只存引言内容（自己标题后到下一个任意级别标题前），
 * 不含子节点正文。避免 embedding 信号重复，便于精确溯源。
 */
@Data
public class ChapterNode {

    /** 章节标题，e.g. "切块策略" */
    private String title;

    /** 标题层级 1-6 对应 H1-H6；0 表示非 markdown 平切片（降级路径） */
    private int headingLevel;

    /** 面包屑路径，e.g. "RAG > 召回 > 切块策略"。根节点时与 title 相同 */
    private String breadcrumb;

    /** 独占式切片正文（自己标题后到下一任意级别标题前的内容） */
    private String content;

    /** LLM 生成的章节摘要（≤200 字）。扁平切片（headingLevel=0）不填，保持 null */
    private String summary;

    /** 源文件起始行号（含），从 1 开始计数 */
    private int lineStart;

    /** 源文件结束行号（含） */
    private int lineEnd;

    /** 插入 DB 后回填的 id，供子节点设置 parent_id */
    private Long dbId;

    /** 子节点列表 */
    private List<ChapterNode> children = new ArrayList<>();

    public ChapterNode addChild(ChapterNode child) {
        children.add(child);
        return this;
    }

    /**
     * 计算 embedding 源文本。
     *
     * <p>优先用 LLM 摘要（{@link #summary}）：解决短节点信号弱、长节点稀释问题。
     * 摘要为空时降级到原 title + breadcrumb + content，保持向后兼容（含扁平切片的 headingLevel=0 路径）。
     *
     * <p>无论是摘要路径还是降级路径，都先拼 title + breadcrumb——保留结构上下文让向量更聚焦。
     *
     * <p>null 安全：扁平切片没有 title/breadcrumb，此时相关字段为 null，
     * {@code appendIfNonEmpty} 守卫避免拼出 "null\nnull\n..." 字符串污染 embedding。
     */
    public String embeddingText() {
        StringBuilder sb = new StringBuilder();
        appendIfNonEmpty(sb, title);
        appendIfNonEmpty(sb, breadcrumb);
        String body = (summary != null && !summary.isEmpty()) ? summary : content;
        if (body != null) {
            sb.append(body);
        }
        return sb.toString();
    }

    private void appendIfNonEmpty(StringBuilder sb, String line) {
        if (line != null && !line.isEmpty()) {
            sb.append(line).append("\n");
        }
    }
}
