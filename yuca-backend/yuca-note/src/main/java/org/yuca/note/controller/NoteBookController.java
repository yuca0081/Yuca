package org.yuca.note.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.yuca.note.dto.request.CreateNoteBookRequest;
import org.yuca.note.dto.request.UpdateNoteBookRequest;
import org.yuca.note.dto.response.NoteBookResponse;
import org.yuca.note.service.NoteBookService;
import org.yuca.common.annotation.RequireLogin;
import org.yuca.common.response.Result;
import org.yuca.infrastructure.security.SecurityUtils;

import java.util.List;

/**
 * 笔记本管理Controller
 */
@Slf4j
@RestController
@RequestMapping("/note/books")
@Tag(name = "笔记本管理", description = "笔记本的增删改查接口")
public class NoteBookController {

    @Autowired
    private NoteBookService noteBookService;

    /**
     * 创建笔记本
     */
    @PostMapping
    @Operation(summary = "创建笔记本")
    @RequireLogin
    public Result<Long> createNoteBook(@Valid @RequestBody CreateNoteBookRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        Long bookId = noteBookService.createNoteBook(request, userId);
        return Result.success(bookId);
    }

    /**
     * 更新笔记本
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新笔记本")
    @RequireLogin
    public Result<Void> updateNoteBook(@PathVariable Long id,
                                        @Valid @RequestBody UpdateNoteBookRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        request.setId(id);
        noteBookService.updateNoteBook(request, userId);
        return Result.success();
    }

    /**
     * 删除笔记本
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除笔记本")
    @RequireLogin
    public Result<Void> deleteNoteBook(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        noteBookService.deleteNoteBook(id, userId);
        return Result.success();
    }

    /**
     * 获取笔记本详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取笔记本详情")
    @RequireLogin
    public Result<NoteBookResponse> getNoteBook(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        NoteBookResponse response = noteBookService.getNoteBook(id, userId);
        return Result.success(response);
    }

    /**
     * 获取用户的笔记本列表
     */
    @GetMapping
    @Operation(summary = "获取笔记本列表")
    @RequireLogin
    public Result<List<NoteBookResponse>> listNoteBooks() {
        Long userId = SecurityUtils.getCurrentUserId();
        List<NoteBookResponse> list = noteBookService.listNoteBooks(userId);
        return Result.success(list);
    }

    /**
     * 设置默认笔记本
     */
    @PostMapping("/{id}/default")
    @Operation(summary = "设置默认笔记本")
    @RequireLogin
    public Result<Void> setAsDefault(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        noteBookService.setAsDefault(id, userId);
        return Result.success();
    }
}
