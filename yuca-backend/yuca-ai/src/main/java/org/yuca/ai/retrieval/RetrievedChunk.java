package org.yuca.ai.retrieval;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 检索到的知识切片
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RetrievedChunk {

    private Long chunkId;

    private Long docId;

    private Long kbId;

    private String content;

    /**
     * 融合后的相关度得分
     */
    private double score;
}
