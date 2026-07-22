package org.yuca.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.yuca.knowledge.entity.KnowledgeVocabulary;

import java.util.List;

/**
 * 查询扩展词汇表 Mapper（#8 查询扩展）。
 *
 * @author Yuca
 * @since 2026-07-22
 */
@Mapper
public interface KnowledgeVocabularyMapper extends BaseMapper<KnowledgeVocabulary> {

    /**
     * 向量相似度搜索：在指定知识库的未删除词汇中，按余弦距离召回 topK。
     *
     * <p>复用 HNSW 索引（idx_vocab_embedding_hnsw），WHERE deleted=0 已被索引部分谓词覆盖。
     *
     * @param kbId           知识库ID
     * @param queryEmbedding 查询向量（PG vector 字面量字符串，如 "[0.1,0.2,...]"）
     * @param topK           返回结果数量
     * @param threshold      相似度阈值（1 - 余弦距离）
     * @return 命中的词汇列表
     */
    List<KnowledgeVocabulary> searchSimilar(@Param("kbId") Long kbId,
                                             @Param("queryEmbedding") String queryEmbedding,
                                             @Param("topK") Integer topK,
                                             @Param("threshold") Double threshold);
}
