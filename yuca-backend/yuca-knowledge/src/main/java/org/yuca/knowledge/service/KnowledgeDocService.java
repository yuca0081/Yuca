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
import org.yuca.ai.agent.Agent;
import org.yuca.ai.agent.AgentFactory;
import org.yuca.ai.config.AiProperties;
import org.yuca.ai.core.message.UserMessage;
import org.yuca.ai.core.model.ChatRequest;
import org.yuca.ai.core.model.ChatResponse;
import org.yuca.ai.embedding.EmbeddingService;
import org.yuca.knowledge.document.ChapterNode;
import org.yuca.common.exception.BusinessException;
import org.yuca.common.response.ErrorCode;
import org.yuca.infrastructure.storage.dto.UploadResult;
import org.yuca.infrastructure.storage.service.FileStorageService;
import org.yuca.knowledge.dto.response.KnowledgeDocResponse;
import org.yuca.knowledge.entity.KnowledgeChunk;
import org.yuca.knowledge.entity.KnowledgeDoc;
import org.yuca.knowledge.entity.KnowledgeBase;
import org.yuca.knowledge.mapper.KnowledgeBaseMapper;
import org.yuca.knowledge.document.parser.DocumentParserRegistry;
import org.yuca.knowledge.mapper.KnowledgeChunkMapper;
import org.yuca.knowledge.mapper.KnowledgeDocMapper;
import org.yuca.knowledge.entity.KnowledgeVocabulary;
import org.yuca.knowledge.mapper.KnowledgeVocabularyMapper;
import org.yuca.knowledge.service.quality.DocumentQualityScorer;
import org.yuca.knowledge.service.quality.DocumentRoutingService;
import org.yuca.knowledge.service.quality.QualityScore;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private DocumentParserRegistry documentParserRegistry;

    @Autowired
    private AgentFactory agentFactory;

    @Autowired
    private AiProperties aiProperties;

    @Autowired
    private KnowledgeVocabularyMapper knowledgeVocabularyMapper;

    @Autowired
    private DocumentQualityScorer documentQualityScorer;

    @Autowired
    private DocumentRoutingService documentRoutingService;

    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB

    /** DashScope text-embedding-v3 单次批量上限 10 条，超出需自行分批 */
    private static final int EMBED_BATCH_SIZE = 10;

    // ========== #8 查询扩展：词汇表自动抽取 ==========
    /** 章节标题长度过滤下限：太短无语义（如单字"一"、"序"） */
    private static final int VOCAB_TERM_MIN_LEN = 2;
    /** 章节标题长度过滤上限：太长通常是正文误识别，不是概念词 */
    private static final int VOCAB_TERM_MAX_LEN = 50;

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

        // 获取文件格式与文件名
        String fileFormat = getFileFormat(file.getOriginalFilename());
        String docName = file.getOriginalFilename();

        // 读取原始字节并计算 SHA256，用于增量更新判断
        byte[] bytes;
        String contentHash;
        try {
            bytes = file.getBytes();
            contentHash = sha256Hex(bytes);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件读取失败: " + e.getMessage());
        }

        // 查找同 (kbId, docName) 的未删除旧文档
        KnowledgeDoc oldDoc = findActiveByName(kbId, docName);

        // 分支 A：内容完全相同 → 跳过解析/上传/嵌入，直接返回旧 docId
        if (oldDoc != null && contentHash.equals(oldDoc.getContentHash())) {
            log.info("[upload] hash 相同，跳过上传: kbId={}, docName={}, oldDocId={}", kbId, docName, oldDoc.getId());
            return oldDoc.getId();
        }

        // 分支 B/C：内容不同或首次上传 → 解析、评分、路由切片
        List<ChapterNode> chapterRoots;
        QualityScore qualityScore;
        try {
            String content = documentParserRegistry.parse(fileFormat, bytes);

            // #11 质量评分：解析后立即评分，供后续路由 + 持久化
            qualityScore = documentQualityScorer.score(content, fileFormat);
            log.info("[upload] 文档质量评分: docName={}, tier={}, overall={}",
                    docName, qualityScore.getTier(), qualityScore.getOverall());

            // #11 智能路由：根据 tier 选切片策略
            // 关闭 quality 开关时 DocumentRoutingService.route 内部退化为原硬编码路径
            chapterRoots = documentRoutingService.route(content, fileFormat, qualityScore);
            log.info("[upload] 切片完成: docName={}, 切片数={}, tier={}",
                    docName, countNodes(chapterRoots), qualityScore.getTier());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("文档解析失败: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文档解析失败: " + e.getMessage());
        }

        // 统计总节点数（含子节点）
        int totalNodes = countNodes(chapterRoots);

        // 上传文件到MinIO（同路径覆盖旧文件，节省存储）
        UploadResult uploadResult;
        try {
            String objectName = "knowledge/" + kbId + "/" + docName;
            uploadResult = fileStorageService.upload(file, objectName);
        } catch (Exception e) {
            log.error("文件上传失败: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件上传失败: " + e.getMessage());
        }

        // 分支 C：hash 不同，触发重建——先软删旧 doc、旧切片和旧词汇
        if (oldDoc != null) {
            log.info("[upload] 检测到内容变更，触发重建: kbId={}, docName={}, oldDocId={}", kbId, docName, oldDoc.getId());
            softDeleteChunksOfDoc(oldDoc.getId());
            softDeleteVocabOfDoc(oldDoc.getId());
            knowledgeDocMapper.deleteById(oldDoc.getId());  // 逻辑删除（@TableLogic 自动转 UPDATE）
        } else {
            log.info("[upload] 新建文档: kbId={}, docName={}", kbId, docName);
        }

        // 插入新文档记录（带 contentHash + 质量评分）
        KnowledgeDoc knowledgeDoc = new KnowledgeDoc();
        knowledgeDoc.setKbId(kbId);
        knowledgeDoc.setDocName(docName);
        knowledgeDoc.setDocFormat(fileFormat);
        knowledgeDoc.setDocSize(file.getSize());
        knowledgeDoc.setFilePath(uploadResult.getObjectName());
        knowledgeDoc.setDataSource("upload");
        knowledgeDoc.setChunkCount(totalNodes);
        knowledgeDoc.setContentHash(contentHash);
        knowledgeDoc.setQualityScore((float) qualityScore.getOverall());
        knowledgeDoc.setQualityTier(qualityScore.getTier());

        knowledgeDocMapper.insert(knowledgeDoc);

        // 批量生成向量并按章节树结构存储切片
        saveChapterNodes(knowledgeDoc.getId(), kbId, chapterRoots);

        // 8 查询扩展：把章节标题抽到 knowledge_vocabulary 表，供检索时找同义词
//        extractAndSaveVocabulary(knowledgeDoc.getId(), kbId, chapterRoots);

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

            // 逻辑删除相关词汇（#8 查询扩展：本 doc 抽出的词汇也属于该 doc 的派生数据）
            softDeleteVocabOfDoc(docId);

            // 同步清理 MinIO 物理文件：失败仅记 WARN，不影响已完成的 DB 软删
            deleteMinioFileSafely(doc.getFilePath(), docId);

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
     *   <li>为章节节点（headingLevel &gt; 0）调 LLM 生成摘要；扁平切片跳过</li>
     *   <li>按节点 embeddingText()（优先 summary，降级 title + breadcrumb + content）批量生成向量</li>
     *   <li>DFS 前序递归插入，MyBatis-Plus 回填 id 用于子节点的 parent_id</li>
     * </ol>
     */
    private void saveChapterNodes(Long docId, Long kbId, List<ChapterNode> roots) {
        if (roots.isEmpty()) {
            log.warn("无切片可保存: docId={}", docId);
            return;
        }

        // 1. DFS 前序扁平化（跳过空 content 节点）
        List<ChapterNode> all = new ArrayList<>();
        for (ChapterNode root : roots) {
            flattenDfs(root, all);
        }
        if (all.isEmpty()) {
            log.warn("过滤空 content 节点后无可保存切片: docId={}", docId);
            return;
        }

        // node -> 在 all 中的索引（用 IdentityHashMap 按==比较，避免 equals 误判）
        Map<ChapterNode, Integer> indexMap = new IdentityHashMap<>();
        for (int i = 0; i < all.size(); i++) {
            indexMap.put(all.get(i), i);
        }

        // 1.5 为章节节点生成 LLM 摘要（扁平切片 headingLevel=0 跳过）
        // 暂时关闭：DashScope qwen-turbo 免费额度耗尽，恢复额度后取消注释即可
        // generateSummaries(all);

        // 2. 批量生成嵌入（embeddingText 此时优先用 summary，分批规避 DashScope 单次 25 条上限）
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
        // 跳过空 content 节点：连续标题（H1 紧接 H2）时父节点 content 必然为空，
        // 其标题语义已通过子节点 breadcrumb 保留，无需单独生成 chunk
        if (node.getContent() != null && !node.getContent().isBlank()) {
            out.add(node);
        }
        for (ChapterNode child : node.getChildren()) {
            flattenDfs(child, out);
        }
    }

    private void insertNodeRecursive(ChapterNode node, Long parentId, Long docId, Long kbId,
                                     List<Double[]> embeddings,
                                     Map<ChapterNode, Integer> indexMap,
                                     int[] chunkIdx) {
        // 与 flattenDfs 同步：空 content 节点不生成 chunk 记录，
        // 但继续递归 children，parentId 透传给子节点（指向最近祖先非空节点）
        Long nextParentId = parentId;
        if (node.getContent() != null && !node.getContent().isBlank()) {
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
                chunk.setSummary(node.getSummary());  // LLM 摘要；失败降级时为 null
                chunk.setLineStart(node.getLineStart());
                chunk.setLineEnd(node.getLineEnd());
            }
            chunk.setParentId(parentId);

            knowledgeChunkMapper.insert(chunk);  // 回填 chunk.id
            node.setDbId(chunk.getId());
            nextParentId = chunk.getId();
        }

        for (ChapterNode child : node.getChildren()) {
            insertNodeRecursive(child, nextParentId, docId, kbId, embeddings, indexMap, chunkIdx);
        }
    }

    /** 分批调 embedding API，规避 DashScope text-embedding-v3 单次 10 条上限 */
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
     * 为所有 headingLevel > 0 的章节节点生成 LLM 摘要。
     *
     * <p>扁平切片（headingLevel=0）跳过——已是固定 100 字小段，再压缩意义不大且增加 N 次 LLM 调用成本。
     *
     * <p>失败降级：单个节点摘要失败时仅记 WARN 日志，{@code summary} 保持 null，
     * 后续 {@link ChapterNode#embeddingText()} 会自动走 title + breadcrumb + content 原文路径。
     */
    private void generateSummaries(List<ChapterNode> nodes) {
        int success = 0, skipped = 0, failed = 0;
        for (ChapterNode node : nodes) {
            if (node.getHeadingLevel() <= 0) {
                skipped++;
                continue;
            }
            try {
                String summary = summarizeNode(node);
                if (summary != null && !summary.isBlank()) {
                    node.setSummary(summary);
                    success++;
                } else {
                    failed++;
                }
            } catch (Exception e) {
                log.warn("[upload] 章节摘要生成失败，降级用原文: title={}, error={}",
                        node.getTitle(), e.getMessage());
                failed++;
            }
        }
        log.info("[upload] 摘要生成完成: total={}, success={}, skipped={}, failed={}",
                nodes.size(), success, skipped, failed);
    }

    /** 调用单轮 Agent 生成单节点摘要。复用 {@code summary.modelName}（qwen-turbo） */
    private String summarizeNode(ChapterNode node) {
        String prompt = buildSummaryPrompt(node);
        ChatRequest req = ChatRequest.builder()
                .messages(List.of(UserMessage.from(prompt)))
                .build();
        Agent agent = agentFactory.simpleAgent(aiProperties.getSummary().getModelName());
        ChatResponse resp = agent.execute(req);
        return resp.aiMessage() == null ? null : resp.aiMessage().text();
    }

    private String buildSummaryPrompt(ChapterNode node) {
        StringBuilder sb = new StringBuilder();
        sb.append("请把以下章节内容压缩为不超过 200 字的摘要，")
                .append("保留关键信息和专有名词，不要解释或评论：\n\n");
        if (node.getTitle() != null) {
            sb.append("标题：").append(node.getTitle()).append("\n");
        }
        if (node.getBreadcrumb() != null) {
            sb.append("路径：").append(node.getBreadcrumb()).append("\n");
        }
        sb.append("内容：\n").append(node.getContent() == null ? "" : node.getContent());
        return sb.toString();
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

    /**
     * 计算字节数组的 SHA256，返回 64 字符小写 hex。
     * HexFormat 是 Java 17+ 的标准 API，比 String.format("%02x") 快一个数量级。
     */
    private String sha256Hex(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(bytes);
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 是 JDK 标准算法，理论上不可能不存在
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "SHA-256 算法不可用: " + e.getMessage());
        }
    }

    /**
     * 按 (kbId, docName) 查找未删除的旧文档。
     * 依赖 KnowledgeDoc 上的 @TableLogic 自动过滤 deleted=1。
     */
    private KnowledgeDoc findActiveByName(Long kbId, String docName) {
        LambdaQueryWrapper<KnowledgeDoc> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeDoc::getKbId, kbId)
                .eq(KnowledgeDoc::getDocName, docName);
        return knowledgeDocMapper.selectOne(wrapper);
    }

    /**
     * 软删某文档下的全部切片（@TableLogic 自动转 UPDATE SET deleted=1）。
     */
    private void softDeleteChunksOfDoc(Long docId) {
        LambdaQueryWrapper<KnowledgeChunk> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeChunk::getDocId, docId);
        knowledgeChunkMapper.delete(wrapper);
    }

    /**
     * 软删某文档抽取的全部词汇（@TableLogic 自动转 UPDATE SET deleted=1）。
     *
     * <p>管理员预设的词汇（doc_id IS NULL）不会被本方法影响。
     */
    private void softDeleteVocabOfDoc(Long docId) {
        LambdaQueryWrapper<KnowledgeVocabulary> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeVocabulary::getDocId, docId);
        knowledgeVocabularyMapper.delete(wrapper);
    }

    /**
     * #8 查询扩展：把章节标题抽到 {@code knowledge_vocabulary} 表。
     *
     * <p>策略：
     * <ul>
     *   <li>DFS 前序收集所有 {@code headingLevel > 0} 的章节标题</li>
     *   <li>长度过滤：[2, 50] 字符——太短无语义，太长通常是误识别</li>
     *   <li>去重：{@code LinkedHashSet} 按小写等值去重，保留首次出现顺序</li>
     *   <li>批量 embedding（复用 {@link #EMBED_BATCH_SIZE}）后逐条插入</li>
     * </ul>
     *
     * <p>失败降级：整体 try-catch + WARN，不阻断上传流程。词汇抽取是"锦上添花"，
     * 不应让一次 embed API 故障导致整个上传事务回滚。
     *
     * <p>扁平切片（roots 全是 headingLevel=0）：titles 为空，直接返回。
     */
    private void extractAndSaveVocabulary(Long docId, Long kbId, List<ChapterNode> roots) {
        try {
            // 1. DFS 收集标题
            Set<String> titles = new LinkedHashSet<>();
            for (ChapterNode root : roots) {
                collectTitlesDfs(root, titles);
            }
            if (titles.isEmpty()) {
                log.debug("[vocab] 无章节标题可抽取: docId={}", docId);
                return;
            }

            // 2. 长度过滤 + 去 null/blank（LinkedHashSet 已去重等值）
            List<String> terms = titles.stream()
                    .filter(t -> t != null && !t.isBlank())
                    .map(String::trim)
                    .filter(t -> t.length() >= VOCAB_TERM_MIN_LEN && t.length() <= VOCAB_TERM_MAX_LEN)
                    .toList();
            if (terms.isEmpty()) {
                log.debug("[vocab] 标题长度过滤后为空: docId={}", docId);
                return;
            }

            // 3. 批量 embed
            List<Double[]> embeddings = embedInBatches(terms);

            // 4. 逐条插入（量小，不必批量 insert）
            for (int i = 0; i < terms.size(); i++) {
                KnowledgeVocabulary vocab = new KnowledgeVocabulary();
                vocab.setKbId(kbId);
                vocab.setDocId(docId);
                vocab.setTerm(terms.get(i));
                vocab.setEmbedding(embeddings.get(i));
                vocab.setSource("extracted");
                knowledgeVocabularyMapper.insert(vocab);
            }

            log.info("[vocab] 词汇抽取成功: docId={}, kbId={}, terms={}", docId, kbId, terms.size());
        } catch (Exception e) {
            log.warn("[vocab] 词汇抽取失败，跳过（不影响上传主流程）: docId={}, error={}",
                    docId, e.getMessage());
        }
    }

    /** DFS 前序收集章节标题（仅 headingLevel > 0 的节点，扁平切片跳过） */
    private void collectTitlesDfs(ChapterNode node, Set<String> out) {
        if (node.getHeadingLevel() > 0 && node.getTitle() != null) {
            out.add(node.getTitle());
        }
        for (ChapterNode child : node.getChildren()) {
            collectTitlesDfs(child, out);
        }
    }

    /**
     * 安全删除 MinIO 文件：失败仅记 WARN 日志，不抛异常。
     *
     * <p>用于 {@link #deleteDocuments} 流程——DB 软删已成功后调用本方法，
     * 即使 MinIO 故障也不回滚事务。残留文件留作后续后台清理 Job 兜底。
     *
     * <p>MinIO {@code removeObject} 本身幂等（对象不存在不抛异常），
     * 这里 catch 是为了防御 MinIO 连接故障、权限错误等基础设施问题。
     */
    private void deleteMinioFileSafely(String objectName, Long docId) {
        if (objectName == null || objectName.isBlank()) {
            log.debug("[delete] file_path 为空，跳过 MinIO 删除: docId={}", docId);
            return;
        }
        try {
            fileStorageService.delete(objectName);
        } catch (Exception e) {
            log.warn("[delete] MinIO 文件删除失败，DB 已软删，留作后台清理: objectName={}, docId={}, error={}",
                    objectName, docId, e.getMessage());
        }
    }
}
