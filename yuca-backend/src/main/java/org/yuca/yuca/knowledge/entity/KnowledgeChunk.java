package org.yuca.yuca.knowledge.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import org.yuca.yuca.knowledge.handler.PGVectorTypeHandler;

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
}
