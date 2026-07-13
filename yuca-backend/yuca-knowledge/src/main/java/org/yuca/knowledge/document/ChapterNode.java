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
     * <p>null 安全：非 markdown 平切片（headingLevel=0）没有 title/breadcrumb，
     * 此时只返回 content，避免拼出 "null\nnull\n..." 字符串污染 embedding。
     */
    public String embeddingText() {
        StringBuilder sb = new StringBuilder();
        if (title != null && !title.isEmpty()) {
            sb.append(title).append("\n");
        }
        if (breadcrumb != null && !breadcrumb.isEmpty()) {
            sb.append(breadcrumb).append("\n");
        }
        sb.append(content);
        return sb.toString();
    }
}
