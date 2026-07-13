package org.yuca.knowledge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.yuca.ai.embedding.EmbeddingService;
import org.yuca.knowledge.document.ChapterNode;
import org.yuca.knowledge.document.Document;
import org.yuca.knowledge.document.DocumentByCharacterSplitter;
import org.yuca.knowledge.document.MarkdownChapterTreeBuilder;
import org.yuca.common.exception.BusinessException;
import org.yuca.common.response.ErrorCode;
import org.yuca.infrastructure.storage.dto.UploadResult;
import org.yuca.infrastructure.storage.service.FileStorageService;
import org.yuca.knowledge.dto.response.KnowledgeDocResponse;
import org.yuca.knowledge.entity.KnowledgeChunk;
import org.yuca.knowledge.entity.KnowledgeDoc;
import org.yuca.knowledge.entity.KnowledgeBase;
import org.yuca.knowledge.mapper.KnowledgeBaseMapper;
import org.yuca.knowledge.mapper.KnowledgeChunkMapper;
import org.yuca.knowledge.mapper.KnowledgeDocMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 知识库文档服务实现
 * 基于 LangChain4j 框架
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
    private FileStorageService fileStorageService;

    @Autowired
    private EmbeddingService embeddingService;

    @Autowired
    private MarkdownChapterTreeBuilder markdownChapterTreeBuilder;

    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB

    /** DashScope text-embedding-v3 单次批量上限 25 条，超出需自行分批 */
    private static final int EMBED_BATCH_SIZE = 25;

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

        // 解析文档为章节树（md）或扁平字符切片（非 md / md 无标题）
        List<ChapterNode> chapterRoots;
        try {
            byte[] bytes = file.getBytes();
            String content = new String(bytes, StandardCharsets.UTF_8);

            boolean useChapterTree = "md".equalsIgnoreCase(fileFormat)
                    && markdownChapterTreeBuilder.hasHeadings(content);
            if (useChapterTree) {
                chapterRoots = markdownChapterTreeBuilder.build(content);
                log.info("Markdown 章节树切片: docName={}, 根节点数={}", file.getOriginalFilename(), chapterRoots.size());
            } else {
                chapterRoots = splitFlat(content);
                log.info("扁平字符切片: docName={}, 切片数={}", file.getOriginalFilename(), chapterRoots.size());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            log.error("文档解析失败: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文档解析失败: " + e.getMessage());
        }

        // 统计总节点数（含子节点）
        int totalNodes = countNodes(chapterRoots);

        // 上传文件到MinIO
        UploadResult uploadResult;
        try {
            String objectName = "knowledge/" + kbId + "/" + file.getOriginalFilename();
            uploadResult = fileStorageService.upload(file, objectName);
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
        knowledgeDoc.setChunkCount(totalNodes);

        knowledgeDocMapper.insert(knowledgeDoc);

        // 批量生成向量并按章节树结构存储切片
        saveChapterNodes(knowledgeDoc.getId(), kbId, chapterRoots);

        log.info("文档上传成功: docId={}, kbId={}, chunks={}", knowledgeDoc.getId(), kbId, totalNodes);
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
     * 扁平字符切片（非 md 或无标题 md 的降级路径）。
     * 把字符切片结果包装为 headingLevel=0 的 ChapterNode，后续与章节树节点共用一套存储逻辑。
     */
    private List<ChapterNode> splitFlat(String content) {
        DocumentByCharacterSplitter splitter = new DocumentByCharacterSplitter(100, 10);
        List<String> chunks = splitter.split(Document.from(content));
        List<ChapterNode> roots = new ArrayList<>();
        for (String chunk : chunks) {
            ChapterNode node = new ChapterNode();
            node.setHeadingLevel(0);
            node.setContent(chunk);
            roots.add(node);
        }
        return roots;
    }

    /** 统计章节树总节点数（含所有子孙） */
    private int countNodes(List<ChapterNode> roots) {
        int count = 0;
        for (ChapterNode root : roots) {
            count += countNodesDfs(root);
        }
        return count;
    }

    private int countNodesDfs(ChapterNode node) {
        int count = 1;
        for (ChapterNode child : node.getChildren()) {
            count += countNodesDfs(child);
        }
        return count;
    }

    /**
     * 章节树切片存储：
     * <ol>
     *   <li>DFS 前序遍历收集所有节点</li>
     *   <li>按节点 embeddingText()（title + breadcrumb + content）批量生成向量</li>
     *   <li>DFS 前序递归插入，MyBatis-Plus 回填 id 用于子节点的 parent_id</li>
     * </ol>
     */
    private void saveChapterNodes(Long docId, Long kbId, List<ChapterNode> roots) {
        if (roots.isEmpty()) {
            log.warn("无切片可保存: docId={}", docId);
            return;
        }

        // 1. DFS 前序扁平化
        List<ChapterNode> all = new ArrayList<>();
        for (ChapterNode root : roots) {
            flattenDfs(root, all);
        }

        // node -> 在 all 中的索引（用 IdentityHashMap 按==比较，避免 equals 误判）
        Map<ChapterNode, Integer> indexMap = new IdentityHashMap<>();
        for (int i = 0; i < all.size(); i++) {
            indexMap.put(all.get(i), i);
        }

        // 2. 批量生成嵌入（分批规避 DashScope 单次 25 条上限）
        List<String> embeddingTexts = all.stream().map(ChapterNode::embeddingText).toList();
        List<Double[]> embeddings = embedInBatches(embeddingTexts);

        // 3. DFS 前序插入：父先于子，回填 id 供子节点 parentId 使用
        int[] chunkIdx = {0};
        for (ChapterNode root : roots) {
            insertNodeRecursive(root, null, docId, kbId, embeddings, indexMap, chunkIdx);
        }

        log.info("章节树切片保存成功: docId={}, 节点数={}", docId, all.size());
    }

    private void flattenDfs(ChapterNode node, List<ChapterNode> out) {
        out.add(node);
        for (ChapterNode child : node.getChildren()) {
            flattenDfs(child, out);
        }
    }

    private void insertNodeRecursive(ChapterNode node, Long parentId, Long docId, Long kbId,
                                     List<Double[]> embeddings,
                                     Map<ChapterNode, Integer> indexMap,
                                     int[] chunkIdx) {
        KnowledgeChunk chunk = new KnowledgeChunk();
        chunk.setDocId(docId);
        chunk.setKbId(kbId);
        chunk.setContent(node.getContent());
        chunk.setEmbedding(embeddings.get(indexMap.get(node)));
        chunk.setChunkIndex(chunkIdx[0]++);
        chunk.setIsActive(true);

        // 章节树字段（headingLevel > 0 时填充；扁平切片 headingLevel=0 时全为 NULL）
        if (node.getHeadingLevel() > 0) {
            chunk.setTitle(node.getTitle());
            chunk.setHeadingLevel((short) node.getHeadingLevel());
            chunk.setBreadcrumb(node.getBreadcrumb());
            chunk.setLineStart(node.getLineStart());
            chunk.setLineEnd(node.getLineEnd());
        }
        chunk.setParentId(parentId);

        knowledgeChunkMapper.insert(chunk);  // 回填 chunk.id
        node.setDbId(chunk.getId());

        for (ChapterNode child : node.getChildren()) {
            insertNodeRecursive(child, chunk.getId(), docId, kbId, embeddings, indexMap, chunkIdx);
        }
    }

    /** 分批调 embedding API，规避 DashScope text-embedding-v3 单次 25 条上限 */
    private List<Double[]> embedInBatches(List<String> texts) {
        List<Double[]> all = new ArrayList<>(texts.size());
        for (int i = 0; i < texts.size(); i += EMBED_BATCH_SIZE) {
            int end = Math.min(i + EMBED_BATCH_SIZE, texts.size());
            List<String> batch = texts.subList(i, end);
            all.addAll(embeddingService.embedBatchAsDoubleArrays(batch));
        }
        return all;
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
