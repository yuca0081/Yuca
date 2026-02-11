package org.yuca.yuca.knowledge.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.yuca.yuca.common.annotation.RequireLogin;
import org.yuca.yuca.common.response.Result;
import org.yuca.yuca.infrastructure.security.SecurityUtils;
import org.yuca.yuca.knowledge.dto.request.CreateKnowledgeBaseRequest;
import org.yuca.yuca.knowledge.dto.request.UpdateKnowledgeBaseRequest;
import org.yuca.yuca.knowledge.dto.response.KnowledgeBaseResponse;
import org.yuca.yuca.knowledge.service.KnowledgeBaseService;

import java.util.List;

/**
 * 知识库管理Controller
 *
 * @author Yuca
 * @since 2025-01-27
 */
@Slf4j
@RestController
@RequestMapping("/knowledge-base")
@Tag(name = "知识库管理", description = "知识库的增删改查接口")
public class KnowledgeBaseController {

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    /**
     * 创建知识库
     */
    @PostMapping("/create")
    @Operation(summary = "创建知识库")
    @RequireLogin
    public Result<Long> createKnowledgeBase(@Valid @RequestBody CreateKnowledgeBaseRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        Long kbId = knowledgeBaseService.createKnowledgeBase(request, userId);
        return Result.success(kbId);
    }

    /**
     * 更新知识库
     */
    @PostMapping("/{id}/update")
    @Operation(summary = "更新知识库")
    @RequireLogin
    public Result<Void> updateKnowledgeBase(@PathVariable Long id,
                                             @Valid @RequestBody UpdateKnowledgeBaseRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        request.setId(id);
        knowledgeBaseService.updateKnowledgeBase(request, userId);
        return Result.success();
    }

    /**
     * 删除知识库
     */
    @PostMapping("/{id}/delete")
    @Operation(summary = "删除知识库")
    @RequireLogin
    public Result<Void> deleteKnowledgeBase(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        knowledgeBaseService.deleteKnowledgeBase(id, userId);
        return Result.success();
    }

    /**
     * 获取知识库详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取知识库详情")
    @RequireLogin
    public Result<KnowledgeBaseResponse> getKnowledgeBase(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        KnowledgeBaseResponse response = knowledgeBaseService.getKnowledgeBase(id, userId);
        return Result.success(response);
    }

    /**
     * 获取知识库列表
     */
    @GetMapping("/list")
    @Operation(summary = "获取知识库列表")
    @RequireLogin
    public Result<List<KnowledgeBaseResponse>> listKnowledgeBases() {
        Long userId = SecurityUtils.getCurrentUserId();
        List<KnowledgeBaseResponse> list = knowledgeBaseService.listKnowledgeBases(userId);
        return Result.success(list);
    }
}
