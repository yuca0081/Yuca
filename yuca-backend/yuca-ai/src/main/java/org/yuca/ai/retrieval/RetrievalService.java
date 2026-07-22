package org.yuca.ai.retrieval;

import java.util.List;

/**
 * 知识检索服务接口
 * 定义在 yuca-ai 模块，实现在 yuca-knowledge 模块，避免循环依赖
 */
public interface RetrievalService {

    /**
     * 从指定知识库检索相关内容。
     *
     * <p>等价于 {@link #retrieve(String, Long, int, MetadataFilter)} 传 {@code null} 过滤器。
     * 保留本方法以保持向后兼容（RagEnhancer 等下游无感）。
     *
     * @param query 查询文本
     * @param kbId  知识库ID
     * @param topN  返回结果数量
     * @return 检索到的知识切片列表，按相关度降序排列
     */
    List<RetrievedChunk> retrieve(String query, Long kbId, int topN);

    /**
     * 从指定知识库检索相关内容，支持按文档元数据过滤。
     *
     * <p>默认实现委派到 3 参版（忽略 filter），保持接口扩展的非破坏性。
     * KnowledgeRetrievalService 覆盖此方法以启用真正的过滤逻辑。
     *
     * @param query  查询文本
     * @param kbId   知识库ID
     * @param topN   返回结果数量
     * @param filter 元数据过滤参数，null 表示不过滤
     * @return 检索到的知识切片列表，按相关度降序排列
     */
    default List<RetrievedChunk> retrieve(String query, Long kbId, int topN, MetadataFilter filter) {
        return retrieve(query, kbId, topN);
    }
}
