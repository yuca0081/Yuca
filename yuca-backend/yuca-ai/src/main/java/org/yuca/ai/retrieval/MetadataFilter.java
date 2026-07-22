package org.yuca.ai.retrieval;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 元数据过滤参数（#10：元数据过滤检索）。
 *
 * <p>用于在向量检索和 BM25 检索时按文档属性做细粒度过滤。所有字段都可选——
 * null 或空表示不限制。多字段同时存在时按 AND 组合。
 *
 * <p>过滤逻辑：
 * <ul>
 *   <li>{@code tags}：文档 tags 数组与 filter.tags 任一命中（PG 数组 overlap 操作符 {@code &&}）</li>
 *   <li>{@code source}：{@code knowledge_doc.data_source} 等值匹配</li>
 *   <li>{@code dateFrom}/{@code dateTo}：{@code knowledge_doc.created_at} 范围（闭区间）</li>
 *   <li>{@code attrs}：{@code knowledge_doc.metadata->>'key' = 'value'} JSONB 属性等值；
 *       key 必须匹配 {@code ^[A-Za-z0-9_]{1,50}$}（Service 层校验防 SQL 注入）</li>
 * </ul>
 *
 * @author Yuca
 * @since 2026-07-22
 */
@Data
public class MetadataFilter {

    /** 标签过滤（OR 语义：文档 tags 任一命中 filter.tags 即保留） */
    private List<String> tags;

    /** 数据来源等值，如 "upload" / "crawl" */
    private String source;

    /** 创建时间下限（含） */
    private LocalDateTime dateFrom;

    /** 创建时间上限（含） */
    private LocalDateTime dateTo;

    /**
     * JSONB 属性等值过滤。key 走白名单（仅字母数字下划线），防 SQL 注入；
     * value 通过 MyBatis #{} 参数绑定。
     */
    private Map<String, String> attrs;
}
