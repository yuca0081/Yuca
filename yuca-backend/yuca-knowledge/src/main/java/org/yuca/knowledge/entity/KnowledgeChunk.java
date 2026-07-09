package org.yuca.knowledge.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import org.yuca.infrastructure.handle.PGVectorTypeHandler;

import java.time.LocalDateTime;

/**
 * 知识库文档切片实体类
 *
 * @author Yuca
 * @since 2025-01-27
 */
@Data
@TableName("knowledge_chunk")
public class KnowledgeChunk {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属文档ID
     */
    private Long docId;

    /**
     * 所属知识库ID（冗余，优化查询）
     */
    private Long kbId;

    /**
     * 文本内容
     */
    private String content;

    /**
     * 向量嵌入（使用Double数组保持精度）
     */
    @TableField(typeHandler = PGVectorTypeHandler.class)
    private Double[] embedding;

    /**
     * 切片序号
     */
    private Integer chunkIndex;

    /**
     * 是否激活
     */
    private Boolean isActive;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 逻辑删除标记（0-未删除，1-已删除）
     */
    @TableLogic
    private Integer deleted;

    // ========== 章节树字段（v1 新增，都可空，非 md 文件的 chunk 这些字段为 NULL） ==========

    /**
     * 章节标题，e.g. "切块策略"。非 md 平切片为 NULL
     */
    private String title;

    /**
     * 标题层级 1-6 对应 H1-H6；NULL 表示非 md 平切片
     */
    private Short headingLevel;

    /**
     * 面包屑路径，e.g. "RAG > 召回 > 切块策略"。根节点时与 title 相同
     */
    private String breadcrumb;

    /**
     * 父节点 DB id；NULL 表示根节点
     */
    private Long parentId;

    /**
     * LLM 摘要（v1 不填，留字段为后续任务）
     */
    private String summary;

    /**
     * 源文件起始行号（含），从 1 开始计数
     */
    private Integer lineStart;

    /**
     * 源文件结束行号（含）
     */
    private Integer lineEnd;
}
