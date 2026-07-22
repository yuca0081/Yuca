package org.yuca.knowledge.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import org.yuca.infrastructure.handle.PGVectorTypeHandler;

import java.time.LocalDateTime;

/**
 * 查询扩展词汇表实体（#8 查询扩展）。
 *
 * <p>词汇来源：章节标题自动抽取（source=extracted, doc_id 非 NULL）或
 * 管理员预设（source=manual, doc_id=NULL）。检索时按 embedding 相似度
 * 找 query 的 top-K 近义词，拼成 OR 串喂给 BM25 路。
 *
 * @author Yuca
 * @since 2026-07-22
 */
@Data
@TableName("knowledge_vocabulary")
public class KnowledgeVocabulary {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属知识库ID */
    private Long kbId;

    /**
     * 所属文档ID；NULL=管理员预设，非NULL=自动抽取（用于级联清理）。
     */
    private Long docId;

    /** 词汇文本，通常是章节标题或领域术语 */
    private String term;

    /** 词汇向量，1024维（使用 Double 数组保持精度，与 KnowledgeChunk 一致） */
    @TableField(typeHandler = PGVectorTypeHandler.class)
    private Double[] embedding;

    /** extracted=自动抽取；manual=管理员预设 */
    private String source;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /** 逻辑删除标记：0-未删除，1-已删除 */
    @TableLogic
    private Integer deleted;
}
