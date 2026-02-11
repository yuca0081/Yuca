package org.yuca.yuca.knowledge.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文档响应DTO
 *
 * @author Yuca
 * @since 2025-01-27
 */
@Data
public class KnowledgeDocResponse {

    /**
     * 文档ID
     */
    private Long id;

    /**
     * 知识库ID
     */
    private Long kbId;

    /**
     * 知识库名称
     */
    private String kbName;

    /**
     * 文档名称
     */
    private String docName;

    /**
     * 文档格式
     */
    private String docFormat;

    /**
     * 文件大小
     */
    private Long docSize;

    /**
     * 文件路径
     */
    private String filePath;

    /**
     * 切片数量
     */
    private Integer chunkCount;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
