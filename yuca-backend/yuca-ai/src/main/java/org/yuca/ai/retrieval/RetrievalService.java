package org.yuca.ai.retrieval;

import java.util.List;

/**
 * 知识检索服务接口
 * 定义在 yuca-ai 模块，实现在 yuca-knowledge 模块，避免循环依赖
 */
public interface RetrievalService {

    /**
     * 从指定知识库检索相关内容
     *
     * @param query 查询文本
     * @param kbId  知识库ID
     * @param topN  返回结果数量
     * @return 检索到的知识切片列表，按相关度降序排列
     */
    List<RetrievedChunk> retrieve(String query, Long kbId, int topN);
}
