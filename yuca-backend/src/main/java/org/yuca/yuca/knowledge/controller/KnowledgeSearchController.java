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
import org.yuca.yuca.knowledge.dto.request.SemanticSearchRequest;
import org.yuca.yuca.knowledge.dto.response.SearchResultResponse;
import org.yuca.yuca.knowledge.service.KnowledgeSearchService;

import java.util.List;

/**
 * 知识库搜索Controller
 *
 * @author Yuca
 * @since 2025-01-27
 */
@Slf4j
@RestController
@RequestMapping("/knowledge-search")
@Tag(name = "知识库搜索", description = "语义搜索接口")
public class KnowledgeSearchController {

    @Autowired
    private KnowledgeSearchService knowledgeSearchService;

    /**
     * 语义搜索
     */
    @PostMapping("/semantic")
    @Operation(summary = "语义搜索")
    @RequireLogin
    public Result<List<SearchResultResponse>> semanticSearch(@Valid @RequestBody SemanticSearchRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        List<SearchResultResponse> results = knowledgeSearchService.semanticSearch(request, userId);
        return Result.success(results);
    }

    /**
     * 全局语义搜索
     */
    @GetMapping("/global")
    @Operation(summary = "全局语义搜索")
    @RequireLogin
    public Result<List<SearchResultResponse>> globalSearch(
            @RequestParam("query") String query,
            @RequestParam(value = "topK", defaultValue = "5") Integer topK,
            @RequestParam(value = "threshold", defaultValue = "0.7") Double threshold) {
        Long userId = SecurityUtils.getCurrentUserId();
        List<SearchResultResponse> results = knowledgeSearchService.globalSearch(query, topK, threshold, userId);
        return Result.success(results);
    }
}
