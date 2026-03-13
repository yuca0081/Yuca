package org.yuca.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.yuca.knowledge.entity.KnowledgeChunk;

import java.util.List;

/**
 * 知识库文档切片Mapper
 *
 * @author Yuca
 * @since 2025-01-27
 */
@Mapper
public interface KnowledgeChunkMapper extends BaseMapper<KnowledgeChunk> {

    /**
     * 向量相似度搜索
     *
     * @param kbId           知识库ID
     * @param queryEmbedding 查询向量
     * @param topK           返回结果数量
     * @param threshold      相似度阈值
     * @return 匹配的切片列表
     */
    List<KnowledgeChunk> searchSimilar(@Param("kbId") Long kbId,
                                        @Param("queryEmbedding") String queryEmbedding,
                                        @Param("topK") Integer topK,
                                        @Param("threshold") Double threshold);

    /**
     * 全局向量相似度搜索（不限制知识库）
     *
     * @param queryEmbedding 查询向量
     * @param topK           返回结果数量
     * @param threshold      相似度阈值
     * @return 匹配的切片列表
     */
    List<KnowledgeChunk> searchSimilarGlobal(@Param("queryEmbedding") String queryEmbedding,
                                              @Param("topK") Integer topK,
                                              @Param("threshold") Double threshold);
}
