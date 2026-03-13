package org.yuca.note.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.yuca.note.dto.request.CreateTagRequest;
import org.yuca.note.dto.request.UpdateTagRequest;
import org.yuca.note.dto.response.NoteItemResponse;
import org.yuca.note.dto.response.NoteTagResponse;
import org.yuca.note.service.NoteItemService;
import org.yuca.note.service.NoteTagService;
import org.yuca.common.annotation.RequireLogin;
import org.yuca.common.response.Result;
import org.yuca.infrastructure.security.SecurityUtils;

import java.util.List;

/**
 * 标签管理Controller
 */
@Slf4j
@RestController
@RequestMapping("/note/tags")
@Tag(name = "标签管理", description = "标签的增删改查接口")
public class NoteTagController {

    @Autowired
    private NoteTagService noteTagService;

    @Autowired
    private NoteItemService noteItemService;

    /**
     * 创建标签
     */
    @PostMapping
    @Operation(summary = "创建标签")
    @RequireLogin
    public Result<Long> createTag(@Valid @RequestBody CreateTagRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        Long tagId = noteTagService.createTag(request, userId);
        return Result.success(tagId);
    }

    /**
     * 更新标签
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新标签")
    @RequireLogin
    public Result<Void> updateTag(@PathVariable Long id,
                                   @Valid @RequestBody UpdateTagRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        request.setId(id);
        noteTagService.updateTag(request, userId);
        return Result.success();
    }

    /**
     * 删除标签
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除标签")
    @RequireLogin
    public Result<Void> deleteTag(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        noteTagService.deleteTag(id, userId);
        return Result.success();
    }

    /**
     * 获取标签详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取标签详情")
    @RequireLogin
    public Result<NoteTagResponse> getTag(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        NoteTagResponse response = noteTagService.getTag(id, userId);
        return Result.success(response);
    }

    /**
     * 获取用户的标签列表
     */
    @GetMapping
    @Operation(summary = "获取标签列表")
    @RequireLogin
    public Result<List<NoteTagResponse>> listTags() {
        Long userId = SecurityUtils.getCurrentUserId();
        List<NoteTagResponse> list = noteTagService.listTags(userId);
        return Result.success(list);
    }

    /**
     * 获取标签下的所有文档
     */
    @GetMapping("/{id}/items")
    @Operation(summary = "获取标签下的所有文档")
    @RequireLogin
    public Result<List<NoteItemResponse>> getDocumentsByTag(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        List<NoteItemResponse> list = noteItemService.getDocumentsByTag(id, userId);
        return Result.success(list);
    }
}
