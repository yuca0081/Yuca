package org.yuca.ai.retrieval;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 检索到的知识切片
 */
@Data
@NoArgsConstructor
public class RetrievedChunk {

    private Long chunkId;

    private Long docId;

    private Long kbId;

    private String content;

    /**
     * 融合后的相关度得分
     */
    private double score;

    /**
     * LLM 摘要，{@link TokenBudgetAssembler} 用作超长降级的第一优先级。
     * 扁平切片（headingLevel=0）或摘要生成失败时为 null。
     */
    private String summary;

    public RetrievedChunk(Long chunkId, Long docId, Long kbId, String content, double score) {
        this(chunkId, docId, kbId, content, score, null);
    }

    public RetrievedChunk(Long chunkId, Long docId, Long kbId, String content, double score, String summary) {
        this.chunkId = chunkId;
        this.docId = docId;
        this.kbId = kbId;
        this.content = content;
        this.score = score;
        this.summary = summary;
    }
}
