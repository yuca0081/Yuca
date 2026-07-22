package org.yuca.knowledge.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.yuca.common.annotation.RequireLogin;
import org.yuca.common.response.Result;
import org.yuca.ai.retrieval.RetrievedChunk;
import org.yuca.ai.retrieval.RetrievalService;
import org.yuca.knowledge.dto.request.MetadataSearchRequest;

import java.util.List;

/**
 * 知识库检索 Controller（#10）。
 *
 * <p>独立暴露检索能力——含元数据过滤参数（tags / source / 时间范围 / JSONB 属性）。
 * 注意：RetrievalService 接口的 3 参版本仍由 RagEnhancer 内部调用（Agent 聊天 RAG 注入），
 * 本 Controller 仅服务于"显式检索"场景（如检索调试 / 独立检索 UI）。
 *
 * @author Yuca
 * @since 2026-07-22
 */
@Slf4j
@RestController
@RequestMapping("/knowledge-search")
@Tag(name = "知识库检索", description = "支持元数据过滤的语义检索接口")
public class KnowledgeSearchController {

    @Autowired
    private RetrievalService retrievalService;

    /**
     * 带元数据过滤的语义检索。所有过滤字段可选；全为空时等价于无过滤检索。
     */
    @PostMapping("/search")
    @Operation(summary = "带元数据过滤的语义检索")
    @RequireLogin
    public Result<List<RetrievedChunk>> search(@RequestBody MetadataSearchRequest request) {
        List<RetrievedChunk> chunks = retrievalService.retrieve(
                request.getQuery(),
                request.getKbId(),
                request.getTopN(),
                request.toFilter());
        return Result.success(chunks);
    }
}
