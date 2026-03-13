package org.yuca.knowledge.dto.response;

import lombok.Data;

/**
 * 语义搜索结果响应DTO
 *
 * @author Yuca
 * @since 2025-01-27
 */
@Data
public class SearchResultResponse {

    /**
     * 切片ID
     */
    private Long chunkId;

    /**
     * 文档ID
     */
    private Long docId;

    /**
     * 文档名称
     */
    private String docName;

    /**
     * 切片内容
     */
    private String content;

    /**
     * 相似度得分
     */
    private Double similarity;

    /**
     * 切片序号
     */
    private Integer chunkIndex;
}
