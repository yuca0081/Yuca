package org.yuca.knowledge.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import org.yuca.infrastructure.handle.StringArrayListTypeHandler;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 知识库文档实体类
 *
 * @author Yuca
 * @since 2025-01-27
 */
@Data
@TableName(value = "knowledge_doc", autoResultMap = true)
public class KnowledgeDoc {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属知识库ID
     */
    private Long kbId;

    /**
     * 文档名称
     */
    private String docName;

    /**
     * 文档格式（md, txt, pdf, docx）
     */
    private String docFormat;

    /**
     * 文件大小（字节）
     */
    private Long docSize;

    /**
     * MinIO存储路径
     */
    private String filePath;

    /**
     * 数据来源
     */
    private String dataSource;

    /**
     * 文档标签数组（#10 元数据过滤）。PG VARCHAR[] ↔ List&lt;String&gt;。
     */
    @TableField(typeHandler = StringArrayListTypeHandler.class)
    private List<String> tags;

    /**
     * 元数据（JSONB 字段的字符串表示；具体 key/value 过滤由 MetadataFilter.attrs 负责）
     */
    private String metadata;

    /**
     * 切片数量
     */
    private Integer chunkCount;

    /**
     * 文件原始字节 SHA256 的 hex 编码（64 字符），用于增量更新时识别"内容是否变化"。
     * 旧记录可能为 NULL。
     */
    private String contentHash;

    /**
     * #11 文档质量评分 [0,1]，越大越好；旧记录为 NULL。
     */
    private Float qualityScore;

    /**
     * #11 质量分类：Clean / Decent / Garbage；旧记录为 NULL。
     */
    private String qualityTier;

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
}
