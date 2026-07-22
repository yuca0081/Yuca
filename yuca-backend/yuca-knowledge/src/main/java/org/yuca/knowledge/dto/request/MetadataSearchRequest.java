package org.yuca.knowledge.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.yuca.ai.retrieval.MetadataFilter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 知识库元数据检索请求（#10）。
 *
 * <p>封装 topN + 所有元数据过滤字段，过滤字段全可选；为 null 或空表示不限制。
 * 内部用 {@link MetadataFilter} 承载过滤逻辑。
 *
 * @author Yuca
 * @since 2026-07-22
 */
@Data
public class MetadataSearchRequest {

    @NotNull(message = "知识库ID不能为空")
    private Long kbId;

    @NotBlank(message = "查询文本不能为空")
    private String query;

    /** 返回结果数量，默认 5 */
    private Integer topN = 5;

    // ========== 元数据过滤字段（全可选） ==========

    /** 标签过滤（OR 语义：文档 tags 任一命中即保留） */
    private List<String> tags;

    /** 数据来源等值，如 "upload" / "crawl" */
    private String source;

    /** 创建时间下限（含） */
    private LocalDateTime dateFrom;

    /** 创建时间上限（含） */
    private LocalDateTime dateTo;

    /** JSONB 属性等值过滤；key 仅允许 [A-Za-z0-9_] 防注入 */
    private Map<String, String> attrs;

    /**
     * 把请求中的过滤字段转成 {@link MetadataFilter}。所有字段为 null/空时返回 null，
     * 让检索走无 JOIN 路径。
     */
    public MetadataFilter toFilter() {
        MetadataFilter filter = new MetadataFilter();
        filter.setTags(tags);
        filter.setSource(source);
        filter.setDateFrom(dateFrom);
        filter.setDateTo(dateTo);
        filter.setAttrs(attrs);
        return filter;
    }
}
