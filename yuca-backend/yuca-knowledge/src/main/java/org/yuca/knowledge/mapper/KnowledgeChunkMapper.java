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

    /**
     * 关键词全文搜索
     *
     * @param kbId  知识库ID
     * @param query 查询关键词
     * @param topK  返回结果数量
     * @return 匹配的切片列表
     */
    List<KnowledgeChunk> searchByKeyword(@Param("kbId") Long kbId,
                                          @Param("query") String query,
                                          @Param("topK") Integer topK);

    /**
     * 按 parent_id 查子节点（用于保守父子注入）。
     *
     * <p>按 chunk_index 升序取前 limit 条，保证"前两个子节点"语义稳定。
     * 复用 idx_chunk_parent 索引（DDL 中已建）。
     *
     * @param parentId 父节点 chunk id
     * @param limit    最多返回条数
     * @return 子节点列表（按 chunk_index 升序）
     */
    List<KnowledgeChunk> selectChildrenByParentId(@Param("parentId") Long parentId,
                                                   @Param("limit") Integer limit);
}
