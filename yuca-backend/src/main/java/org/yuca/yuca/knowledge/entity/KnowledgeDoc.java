package org.yuca.yuca.knowledge.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识库文档实体类
 *
 * @author Yuca
 * @since 2025-01-27
 */
@Data
@TableName("knowledge_doc")
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
     * 元数据（JSON格式）
     */
    private String metadata;

    /**
     * 切片数量
     */
    private Integer chunkCount;

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
