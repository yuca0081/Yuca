package org.yuca.yuca.knowledge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yuca.yuca.common.exception.BusinessException;
import org.yuca.yuca.common.response.ErrorCode;
import org.yuca.yuca.knowledge.dto.request.SemanticSearchRequest;
import org.yuca.yuca.knowledge.dto.response.SearchResultResponse;
import org.yuca.yuca.knowledge.entity.KnowledgeBase;
import org.yuca.yuca.knowledge.entity.KnowledgeChunk;
import org.yuca.yuca.knowledge.entity.KnowledgeDoc;
import org.yuca.yuca.knowledge.mapper.KnowledgeBaseMapper;
import org.yuca.yuca.knowledge.mapper.KnowledgeChunkMapper;
import org.yuca.yuca.knowledge.mapper.KnowledgeDocMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 知识库搜索服务
 *
 * @author Yuca
 * @since 2025-01-27
 */
@Slf4j
@Service
public class KnowledgeSearchService {

    @Autowired
    private KnowledgeChunkMapper knowledgeChunkMapper;

    @Autowired
    private KnowledgeBaseMapper knowledgeBaseMapper;

    @Autowired
    private KnowledgeDocMapper knowledgeDocMapper;

    @Autowired
    private EmbeddingService embeddingService;

    /**
     * 语义搜索
     *
     * @param request 搜索请求
     * @param userId  用户ID
     * @return 搜索结果列表
     */
    public List<SearchResultResponse> semanticSearch(SemanticSearchRequest request, Long userId) {
        // 验证知识库权限
        KnowledgeBase knowledgeBase = knowledgeBaseMapper.selectById(request.getKbId());
        if (knowledgeBase == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "知识库不存在");
        }

        if (!knowledgeBase.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问此知识库");
        }

        // 生成查询向量
        Double[] queryEmbedding = embeddingService.embed(request.getQuery());
        String queryVector = embeddingService.formatToPgVector(queryEmbedding);

        // 向量搜索
        List<KnowledgeChunk> chunks = knowledgeChunkMapper.searchSimilar(
                request.getKbId(),
                queryVector,
                request.getTopK(),
                request.getThreshold()
        );

        if (chunks.isEmpty()) {
            log.info("未找到相关内容: kbId={}, query={}", request.getKbId(), request.getQuery());
            return new ArrayList<>();
        }

        // 获取文档信息
        List<Long> docIds = chunks.stream()
                .map(KnowledgeChunk::getDocId)
                .distinct()
                .collect(Collectors.toList());

        LambdaQueryWrapper<KnowledgeDoc> docWrapper = new LambdaQueryWrapper<>();
        docWrapper.in(KnowledgeDoc::getId, docIds);
        List<KnowledgeDoc> docs = knowledgeDocMapper.selectList(docWrapper);

        Map<Long, KnowledgeDoc> docMap = docs.stream()
                .collect(Collectors.toMap(KnowledgeDoc::getId, doc -> doc));

        // 转换为响应DTO
        List<SearchResultResponse> responses = new ArrayList<>();
        for (KnowledgeChunk chunk : chunks) {
            SearchResultResponse response = new SearchResultResponse();
            response.setChunkId(chunk.getId());
            response.setDocId(chunk.getDocId());
            response.setChunkIndex(chunk.getChunkIndex());
            response.setContent(chunk.getContent());

            KnowledgeDoc doc = docMap.get(chunk.getDocId());
            if (doc != null) {
                response.setDocName(doc.getDocName());
            }

            // 计算相似度
            Double similarity = embeddingService.cosineSimilarity(queryEmbedding, chunk.getEmbedding());
            response.setSimilarity(similarity);

            responses.add(response);
        }

        log.info("语义搜索完成: kbId={}, query={}, results={}", request.getKbId(), request.getQuery(), responses.size());
        return responses;
    }

    /**
     * 全局语义搜索（不限制知识库）
     *
     * @param query     搜索查询
     * @param topK      返回结果数量
     * @param threshold 相似度阈值
     * @param userId    用户ID
     * @return 搜索结果列表
     */
    public List<SearchResultResponse> globalSearch(String query, Integer topK, Double threshold, Long userId) {
        // 生成查询向量
        Double[] queryEmbedding = embeddingService.embed(query);
        String queryVector = embeddingService.formatToPgVector(queryEmbedding);

        // 全局向量搜索
        List<KnowledgeChunk> chunks = knowledgeChunkMapper.searchSimilarGlobal(
                queryVector,
                topK,
                threshold
        );

        if (chunks.isEmpty()) {
            log.info("全局搜索未找到相关内容: query={}", query);
            return new ArrayList<>();
        }

        // 验证权限（只能搜索用户自己的知识库）
        List<Long> kbIds = chunks.stream()
                .map(KnowledgeChunk::getKbId)
                .distinct()
                .collect(Collectors.toList());

        LambdaQueryWrapper<KnowledgeBase> kbWrapper = new LambdaQueryWrapper<>();
        kbWrapper.in(KnowledgeBase::getId, kbIds)
                .eq(KnowledgeBase::getUserId, userId);
        List<KnowledgeBase> userKbs = knowledgeBaseMapper.selectList(kbWrapper);

        List<Long> userKbIds = userKbs.stream()
                .map(KnowledgeBase::getId)
                .collect(Collectors.toList());

        // 过滤结果
        List<KnowledgeChunk> filteredChunks = chunks.stream()
                .filter(chunk -> userKbIds.contains(chunk.getKbId()))
                .collect(Collectors.toList());

        // 获取文档信息
        List<Long> docIds = filteredChunks.stream()
                .map(KnowledgeChunk::getDocId)
                .distinct()
                .collect(Collectors.toList());

        LambdaQueryWrapper<KnowledgeDoc> docWrapper = new LambdaQueryWrapper<>();
        docWrapper.in(KnowledgeDoc::getId, docIds);
        List<KnowledgeDoc> docs = knowledgeDocMapper.selectList(docWrapper);

        Map<Long, KnowledgeDoc> docMap = docs.stream()
                .collect(Collectors.toMap(KnowledgeDoc::getId, doc -> doc));

        // 转换为响应DTO
        List<SearchResultResponse> responses = new ArrayList<>();
        for (KnowledgeChunk chunk : filteredChunks) {
            SearchResultResponse response = new SearchResultResponse();
            response.setChunkId(chunk.getId());
            response.setDocId(chunk.getDocId());
            response.setChunkIndex(chunk.getChunkIndex());
            response.setContent(chunk.getContent());

            KnowledgeDoc doc = docMap.get(chunk.getDocId());
            if (doc != null) {
                response.setDocName(doc.getDocName());
            }

            // 计算相似度
            Double similarity = embeddingService.cosineSimilarity(queryEmbedding, chunk.getEmbedding());
            response.setSimilarity(similarity);

            responses.add(response);
        }

        log.info("全局搜索完成: query={}, results={}", query, responses.size());
        return responses;
    }
}
