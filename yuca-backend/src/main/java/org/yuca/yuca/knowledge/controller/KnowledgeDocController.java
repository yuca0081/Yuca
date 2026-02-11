package org.yuca.yuca.knowledge.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.yuca.yuca.common.annotation.RequireLogin;
import org.yuca.yuca.common.response.Result;
import org.yuca.yuca.infrastructure.security.SecurityUtils;
import org.yuca.yuca.knowledge.dto.response.KnowledgeDocResponse;
import org.yuca.yuca.knowledge.service.KnowledgeDocService;

import java.util.List;

/**
 * 知识库文档管理Controller
 *
 * @author Yuca
 * @since 2025-01-27
 */
@Slf4j
@RestController
@RequestMapping("/knowledge-doc")
@Tag(name = "知识库文档管理", description = "文档上传、查询、删除接口")
public class KnowledgeDocController {

    @Autowired
    private KnowledgeDocService knowledgeDocService;

    /**
     * 上传文档
     */
    @PostMapping("/upload")
    @Operation(summary = "上传文档")
    @RequireLogin
    public Result<Long> uploadDocument(@RequestParam("kbId") Long kbId,
                                       @RequestParam("file") MultipartFile file) {
        Long userId = SecurityUtils.getCurrentUserId();
        Long docId = knowledgeDocService.uploadDocument(kbId, file, userId);
        return Result.success(docId);
    }

    /**
     * 分页查询文档列表
     */
    @GetMapping("/pageList")
    @Operation(summary = "分页查询文档列表")
    @RequireLogin
    public Result<IPage<KnowledgeDocResponse>> pageDocuments(
            @RequestParam("kbId") Long kbId,
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam(value = "size", defaultValue = "10") Integer size) {
        Long userId = SecurityUtils.getCurrentUserId();
        IPage<KnowledgeDocResponse> page = knowledgeDocService.pageDocuments(kbId, current, size, userId);
        return Result.success(page);
    }

    /**
     * 批量删除文档
     */
    @PostMapping("/delete")
    @Operation(summary = "批量删除文档")
    @RequireLogin
    public Result<Void> deleteDocuments(@RequestBody List<Long> docIds) {
        Long userId = SecurityUtils.getCurrentUserId();
        knowledgeDocService.deleteDocuments(docIds, userId);
        return Result.success();
    }

    /**
     * 获取文档切片列表
     */
    @GetMapping("/{docId}/chunks")
    @Operation(summary = "获取文档切片列表")
    @RequireLogin
    public Result<List<org.yuca.yuca.knowledge.entity.KnowledgeChunk>> getChunks(
            @PathVariable Long docId) {
        Long userId = SecurityUtils.getCurrentUserId();
        List<org.yuca.yuca.knowledge.entity.KnowledgeChunk> chunks = knowledgeDocService.getChunks(docId, userId);
        return Result.success(chunks);
    }
}
