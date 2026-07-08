package org.yuca.note.tool;

import org.yuca.ai.core.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.yuca.infrastructure.security.SecurityUtils;
import org.yuca.note.entity.NoteItem;
import org.yuca.note.mapper.NoteItemMapper;
import org.yuca.note.service.NoteBookService;
import org.yuca.note.service.NoteItemService;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 笔记工具
 * 暴露给 AI 模型的工具，模型通过这些工具读写用户的笔记数据
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NoteTool {

    private final NoteItemService noteItemService;
    private final NoteBookService noteBookService;
    private final NoteItemMapper noteItemMapper;

    @Tool("搜索用户的笔记。根据关键词搜索笔记标题和内容，返回匹配的笔记列表（包含标题和内容摘要）")
    public String searchNotes(String query) {
        long userId = SecurityUtils.getCurrentUserId();
        try {
            List<NoteItem> items = noteItemMapper.searchDocuments(userId, query, 10);
            if (items.isEmpty()) {
                return "没有找到与 \"" + query + "\" 相关的笔记";
            }
            return items.stream()
                    .map(item -> {
                        String content = item.getContent();
                        String summary = content != null && content.length() > 200
                                ? content.substring(0, 200) + "..."
                                : (content != null ? content : "");
                        return String.format("[ID:%d] %s\n%s", item.getId(), item.getTitle(), summary);
                    })
                    .collect(Collectors.joining("\n\n"));
        } catch (Exception e) {
            log.error("AI工具搜索笔记失败: userId={}", userId, e);
            return "搜索失败: " + e.getMessage();
        }
    }

    @Tool("获取指定笔记的完整内容。noteItemId为笔记ID")
    public String getNoteContent(long noteItemId) {
        long userId = SecurityUtils.getCurrentUserId();
        try {
            NoteItem item = noteItemMapper.selectById(noteItemId);
            if (item == null || !item.getUserId().equals(userId) || item.isFolder()) {
                return "笔记不存在或无权访问";
            }
            return String.format("标题: %s\n\n%s", item.getTitle(),
                    item.getContent() != null ? item.getContent() : "");
        } catch (Exception e) {
            log.error("AI工具获取笔记内容失败: userId={}, noteItemId={}", userId, noteItemId, e);
            return "获取失败: " + e.getMessage();
        }
    }

    @Tool("列出用户最近编辑的笔记")
    public String listRecentNotes() {
        long userId = SecurityUtils.getCurrentUserId();
        try {
            List<NoteItem> items = noteItemMapper.getRecentDocuments(userId, 10);
            if (items.isEmpty()) {
                return "暂无笔记";
            }
            return items.stream()
                    .map(item -> String.format("[ID:%d] %s (更新于 %s, %d字)",
                            item.getId(),
                            item.getTitle(),
                            item.getUpdatedAt() != null ? item.getUpdatedAt().toString() : "未知",
                            item.getWordCount() != null ? item.getWordCount() : 0))
                    .collect(Collectors.joining("\n"));
        } catch (Exception e) {
            log.error("AI工具列出最近笔记失败: userId={}", userId, e);
            return "获取失败: " + e.getMessage();
        }
    }

    @Tool("更新指定笔记的内容。noteItemId为笔记ID，content为新的内容（会完全替换原有内容）")
    public String updateNoteContent(long noteItemId, String content) {
        long userId = SecurityUtils.getCurrentUserId();
        try {
            NoteItem item = noteItemMapper.selectById(noteItemId);
            if (item == null || !item.getUserId().equals(userId) || item.isFolder()) {
                return "笔记不存在或无权访问";
            }
            item.setContent(content);
            item.setWordCount(content != null ? content.length() : 0);
            noteItemMapper.updateById(item);
            log.info("AI工具更新笔记内容成功: userId={}, noteItemId={}", userId, noteItemId);
            return "更新成功，笔记ID: " + noteItemId;
        } catch (Exception e) {
            log.error("AI工具更新笔记内容失败: userId={}, noteItemId={}", userId, noteItemId, e);
            return "更新失败: " + e.getMessage();
        }
    }

    @Tool("列出用户的所有笔记本")
    public String listNoteBooks() {
        long userId = SecurityUtils.getCurrentUserId();
        try {
            var books = noteBookService.listNoteBooks(userId);
            if (books.isEmpty()) {
                return "暂无笔记本";
            }
            return books.stream()
                    .map(b -> String.format("[ID:%d] %s (%d篇笔记%s)",
                            b.getId(),
                            b.getName(),
                            b.getItemCount() != null ? b.getItemCount() : 0,
                            b.getIsDefault() != null && b.getIsDefault() ? ", 默认" : ""))
                    .collect(Collectors.joining("\n"));
        } catch (Exception e) {
            log.error("AI工具列出笔记本失败: userId={}", userId, e);
            return "获取失败: " + e.getMessage();
        }
    }
}
