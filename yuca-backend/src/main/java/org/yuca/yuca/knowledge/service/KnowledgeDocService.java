package org.yuca.yuca.knowledge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.yuca.yuca.common.exception.BusinessException;
import org.yuca.yuca.common.response.ErrorCode;
import org.yuca.yuca.infrastructure.storage.service.FileStorageService;
import org.yuca.yuca.infrastructure.storage.dto.UploadResult;
import org.yuca.yuca.knowledge.dto.response.KnowledgeDocResponse;
import org.yuca.yuca.knowledge.entity.KnowledgeBase;
import org.yuca.yuca.knowledge.entity.KnowledgeChunk;
import org.yuca.yuca.knowledge.entity.KnowledgeDoc;
import org.yuca.yuca.knowledge.mapper.KnowledgeDocMapper;
import org.yuca.yuca.knowledge.mapper.KnowledgeBaseMapper;
import org.yuca.yuca.knowledge.mapper.KnowledgeChunkMapper;
import org.yuca.yuca.knowledge.util.DocumentParser;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 知识库文档服务实现
 *
 * @author Yuca
 * @since 2025-01-27
 */
@Slf4j
@Service
public class KnowledgeDocService extends ServiceImpl<KnowledgeDocMapper, KnowledgeDoc> {

    @Autowired
    private KnowledgeDocMapper knowledgeDocMapper;

    @Autowired
    private KnowledgeBaseMapper knowledgeBaseMapper;

    @Autowired
    private KnowledgeChunkMapper knowledgeChunkMapper;

    @Autowired
    private EmbeddingService embeddingService;

    @Autowired
    private FileStorageService fileStorageService;

    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB

    /**
     * 上传文档并处理
     *
     * @param kbId  知识库ID
     * @param file  文件
     * @param userId 用户ID
     * @return 文档ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long uploadDocument(Long kbId, MultipartFile file, Long userId) {
        // 验证知识库
        KnowledgeBase knowledgeBase = knowledgeBaseMapper.selectById(kbId);
        if (knowledgeBase == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "知识库不存在");
        }

        if (!knowledgeBase.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作此知识库");
        }

        // 验证文件
        validateFile(file);

        // 获取文件格式
        String fileFormat = getFileFormat(file.getOriginalFilename());

        // 解析文档并切片
        List<Document> chunks;
        try {
            chunks = DocumentParser.parseAndSplit(file.getBytes(), file.getOriginalFilename(), fileFormat);
        } catch (Exception e) {
            log.error("文档解析失败: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文档解析失败: " + e.getMessage());
        }

        // 上传文件到MinIO
        UploadResult uploadResult;
        try {
            uploadResult = fileStorageService.upload(file, "knowledge/" + kbId);
        } catch (Exception e) {
            log.error("文件上传失败: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件上传失败: " + e.getMessage());
        }

        // 创建文档记录
        KnowledgeDoc knowledgeDoc = new KnowledgeDoc();
        knowledgeDoc.setKbId(kbId);
        knowledgeDoc.setDocName(file.getOriginalFilename());
        knowledgeDoc.setDocFormat(fileFormat);
        knowledgeDoc.setDocSize(file.getSize());
        knowledgeDoc.setFilePath(uploadResult.getObjectName());
        knowledgeDoc.setDataSource("upload");
        knowledgeDoc.setChunkCount(chunks.size());

        knowledgeDocMapper.insert(knowledgeDoc);

        // 生成向量并存储切片
        saveChunks(knowledgeDoc.getId(), kbId, chunks);

        log.info("文档上传成功: docId={}, kbId={}, chunks={}", knowledgeDoc.getId(), kbId, chunks.size());
        return knowledgeDoc.getId();
    }

    /**
     * 批量删除文档
     *
     * @param docIds 文档ID列表
     * @param userId 用户ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteDocuments(List<Long> docIds, Long userId) {
        for (Long docId : docIds) {
            KnowledgeDoc doc = knowledgeDocMapper.selectById(docId);
            if (doc == null) {
                continue;
            }

            // 验证权限
            KnowledgeBase kb = knowledgeBaseMapper.selectById(doc.getKbId());
            if (kb == null || !kb.getUserId().equals(userId)) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作此文档");
            }

            // 逻辑删除文档
            knowledgeDocMapper.deleteById(docId);

            // 逻辑删除相关切片
            LambdaQueryWrapper<KnowledgeChunk> chunkWrapper = new LambdaQueryWrapper<>();
            chunkWrapper.eq(KnowledgeChunk::getDocId, docId);
            knowledgeChunkMapper.delete(chunkWrapper);

            log.info("文档删除成功: docId={}", docId);
        }
    }

    /**
     * 分页查询文档列表
     *
     * @param kbId      知识库ID
     * @param current   当前页
     * @param size      每页大小
     * @param userId    用户ID
     * @return 文档分页列表
     */
    public IPage<KnowledgeDocResponse> pageDocuments(Long kbId, Integer current, Integer size, Long userId) {
        // 验证知识库权限
        KnowledgeBase knowledgeBase = knowledgeBaseMapper.selectById(kbId);
        if (knowledgeBase == null || !knowledgeBase.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问此知识库");
        }

        Page<KnowledgeDoc> page = new Page<>(current, size);
        LambdaQueryWrapper<KnowledgeDoc> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeDoc::getKbId, kbId)
                .orderByDesc(KnowledgeDoc::getCreatedAt);

        IPage<KnowledgeDoc> docPage = knowledgeDocMapper.selectPage(page, wrapper);

        // 转换为响应DTO
        IPage<KnowledgeDocResponse> responsePage = new Page<>(current, size, docPage.getTotal());
        List<KnowledgeDocResponse> responses = docPage.getRecords().stream()
                .map(doc -> {
                    KnowledgeDocResponse response = new KnowledgeDocResponse();
                    response.setId(doc.getId());
                    response.setKbId(doc.getKbId());
                    response.setKbName(knowledgeBase.getName());
                    response.setDocName(doc.getDocName());
                    response.setDocFormat(doc.getDocFormat());
                    response.setDocSize(doc.getDocSize());
                    response.setFilePath(doc.getFilePath());
                    response.setChunkCount(doc.getChunkCount());
                    response.setCreatedAt(doc.getCreatedAt());
                    return response;
                })
                .collect(Collectors.toList());

        responsePage.setRecords(responses);
        return responsePage;
    }

    /**
     * 获取文档切片列表
     *
     * @param docId  文档ID
     * @param userId 用户ID
     * @return 切片列表
     */
    public List<KnowledgeChunk> getChunks(Long docId, Long userId) {
        // 验证文档存在
        KnowledgeDoc doc = knowledgeDocMapper.selectById(docId);
        if (doc == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文档不存在");
        }

        // 验证权限
        KnowledgeBase kb = knowledgeBaseMapper.selectById(doc.getKbId());
        if (kb == null || !kb.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问此文档");
        }

        // 查询切片列表
        LambdaQueryWrapper<KnowledgeChunk> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeChunk::getDocId, docId)
                .orderByAsc(KnowledgeChunk::getChunkIndex);

        return knowledgeChunkMapper.selectList(wrapper);
    }

    /**
     * 保存文档切片
     */
    private void saveChunks(Long docId, Long kbId, List<Document> chunks) {
        List<String> texts = chunks.stream()
                .map(Document::getText)
                .collect(Collectors.toList());

        // 批量生成向量
        List<Double[]> embeddings = embeddingService.batchEmbed(texts);

        // 保存切片
        for (int i = 0; i < chunks.size(); i++) {
            KnowledgeChunk chunk = new KnowledgeChunk();
            chunk.setDocId(docId);
            chunk.setKbId(kbId);
            chunk.setContent(chunks.get(i).getText());
            chunk.setEmbedding(embeddings.get(i));
            chunk.setChunkIndex(i);
            chunk.setIsActive(true);

            knowledgeChunkMapper.insert(chunk);
        }

        log.info("切片保存成功: docId={}, chunkCount={}", docId, chunks.size());
    }

    /**
     * 验证文件
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件不能为空");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件大小超过限制（最大50MB）");
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件名不能为空");
        }

        String fileFormat = getFileFormat(fileName);
        if (!DocumentParser.isFormatSupported(fileFormat)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "不支持的文件格式: " + fileFormat);
        }
    }

    /**
     * 获取文件格式
     */
    private String getFileFormat(String fileName) {
        int lastDotIndex = fileName.lastIndexOf(".");
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            return fileName.substring(lastDotIndex + 1).toLowerCase();
        }
        return "txt";
    }
}
