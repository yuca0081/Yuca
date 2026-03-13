package org.yuca.note.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.yuca.note.dto.request.AddTagsRequest;
import org.yuca.note.dto.request.CreateItemRequest;
import org.yuca.note.dto.request.MoveItemRequest;
import org.yuca.note.dto.request.UpdateItemRequest;
import org.yuca.note.dto.response.NoteItemResponse;
import org.yuca.note.dto.response.NoteTagResponse;
import org.yuca.note.dto.response.NoteTreeResponse;
import org.yuca.note.service.NoteItemService;
import org.yuca.common.annotation.RequireLogin;
import org.yuca.common.response.Result;
import org.yuca.infrastructure.security.SecurityUtils;
import org.yuca.note.entity.NoteItem;

import java.util.List;

/**
 * 节点管理Controller（单表设计：文件夹和文档统一管理）
 */
@Slf4j
@RestController
@RequestMapping("/note/items")
@Tag(name = "节点管理", description = "文件夹和文档的统一管理接口")
public class NoteItemController {

    @Autowired
    private NoteItemService noteItemService;

    /**
     * 创建节点（文件夹或文档）
     */
    @PostMapping
    @Operation(summary = "创建节点")
    @RequireLogin
    public Result<Long> createItem(@Valid @RequestBody CreateItemRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        Long itemId = noteItemService.createItem(request, userId);
        return Result.success(itemId);
    }

    /**
     * 更新节点
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新节点")
    @RequireLogin
    public Result<Void> updateItem(@PathVariable Long id,
                                    @Valid @RequestBody UpdateItemRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        noteItemService.updateItem(id, request, userId);
        return Result.success();
    }

    /**
     * 删除节点
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除节点")
    @RequireLogin
    public Result<Void> deleteItem(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        noteItemService.deleteItem(id, userId);
        return Result.success();
    }

    /**
     * 移动节点
     */
    @PostMapping("/{id}/move")
    @Operation(summary = "移动节点")
    @RequireLogin
    public Result<Void> moveItem(@PathVariable Long id,
                                  @Valid @RequestBody MoveItemRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        noteItemService.moveItem(id, request, userId);
        return Result.success();
    }

    /**
     * 获取节点详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取节点详情")
    @RequireLogin
    public Result<NoteItemResponse> getItem(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        NoteItemResponse response = noteItemService.getItem(id, userId);
        return Result.success(response);
    }

    /**
     * 获取笔记本的完整树形结构
     */
    @GetMapping("/books/{bookId}/tree")
    @Operation(summary = "获取笔记本树形结构")
    @RequireLogin
    public Result<NoteTreeResponse> getTree(@PathVariable Long bookId) {
        Long userId = SecurityUtils.getCurrentUserId();
        NoteTreeResponse response = noteItemService.getTree(bookId, userId);
        return Result.success(response);
    }

    /**
     * 获取直接子节点列表
     */
    @GetMapping("/{id}/children")
    @Operation(summary = "获取直接子节点列表")
    @RequireLogin
    public Result<List<NoteItemResponse>> getChildren(
            @PathVariable Long id,
            @Parameter(description = "笔记本ID") @RequestParam Long bookId) {
        Long userId = SecurityUtils.getCurrentUserId();
        List<NoteItemResponse> list = noteItemService.getChildren(id, bookId, userId);
        return Result.success(list);
    }

    /**
     * 获取最近编辑的文档
     */
    @GetMapping("/recent")
    @Operation(summary = "获取最近编辑的文档")
    @RequireLogin
    public Result<List<NoteItemResponse>> getRecentDocuments(
            @Parameter(description = "数量限制") @RequestParam(defaultValue = "10") int limit) {
        Long userId = SecurityUtils.getCurrentUserId();
        List<NoteItemResponse> list = noteItemService.getRecentDocuments(userId, limit);
        return Result.success(list);
    }

    /**
     * 获取置顶文档
     */
    @GetMapping("/pinned")
    @Operation(summary = "获取置顶文档")
    @RequireLogin
    public Result<List<NoteItemResponse>> getPinnedDocuments() {
        Long userId = SecurityUtils.getCurrentUserId();
        List<NoteItemResponse> list = noteItemService.getPinnedDocuments(userId);
        return Result.success(list);
    }

    // ========== 标签管理接口 ==========

    /**
     * 给文档添加标签
     */
    @PostMapping("/{id}/tags")
    @Operation(summary = "给文档添加标签")
    @RequireLogin
    public Result<Void> addTags(@PathVariable Long id,
                                @Valid @RequestBody AddTagsRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        noteItemService.addTags(id, request, userId);
        return Result.success();
    }

    /**
     * 移除文档标签
     */
    @DeleteMapping("/{id}/tags/{tagId}")
    @Operation(summary = "移除文档标签")
    @RequireLogin
    public Result<Void> removeTag(@PathVariable Long id,
                                   @PathVariable Long tagId) {
        Long userId = SecurityUtils.getCurrentUserId();
        noteItemService.removeTag(id, tagId, userId);
        return Result.success();
    }

    /**
     * 获取文档的标签列表
     */
    @GetMapping("/{id}/tags")
    @Operation(summary = "获取文档的标签列表")
    @RequireLogin
    public Result<List<NoteTagResponse>> getItemTags(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        List<NoteTagResponse> list = noteItemService.getItemTags(id, userId);
        return Result.success(list);
    }

    // ========== 批量操作接口 ==========

    /**
     * 批量更新节点排序
     */
    @PostMapping("/batch-sort")
    @Operation(summary = "批量更新节点排序")
    @RequireLogin
    public Result<Void> batchUpdateSort(@RequestBody List<NoteItem> items) {
        Long userId = SecurityUtils.getCurrentUserId();
        noteItemService.batchUpdateSort(items, userId);
        return Result.success();
    }
}
