package org.yuca.yuca.note.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.yuca.yuca.note.entity.NoteItem;

import java.util.List;

/**
 * 节点Mapper接口
 */
@Mapper
public interface NoteItemMapper extends BaseMapper<NoteItem> {

    /**
     * 获取笔记本的完整树形结构（递归查询）
     */
    @Select("""
        WITH RECURSIVE item_tree AS (
            -- 根节点：笔记本下的直接子项
            SELECT * FROM note_item
            WHERE book_id = #{bookId}
              AND parent_id IS NULL
              AND deleted = 0
            UNION ALL
            -- 递归：获取所有子节点
            SELECT i.* FROM note_item i
            INNER JOIN item_tree t ON i.parent_id = t.id
            WHERE i.deleted = 0
        )
        SELECT * FROM item_tree
        ORDER BY parent_id NULLS FIRST, sort_order ASC
    """)
    List<NoteItem> getTreeByBookId(@Param("bookId") Long bookId);

    /**
     * 获取某个节点的所有子孙节点
     */
    @Select("""
        WITH RECURSIVE item_tree AS (
            SELECT * FROM note_item WHERE id = #{itemId}
            UNION ALL
            SELECT i.* FROM note_item i
            INNER JOIN item_tree t ON i.parent_id = t.id
            WHERE i.deleted = 0
        )
        SELECT * FROM item_tree WHERE id != #{itemId}
    """)
    List<NoteItem> getDescendants(@Param("itemId") Long itemId);

    /**
     * 获取直接子节点列表
     */
    @Select("""
        SELECT * FROM note_item
        WHERE parent_id = #{parentId}
          AND book_id = #{bookId}
          AND deleted = 0
        ORDER BY sort_order ASC, created_at DESC
    """)
    List<NoteItem> getChildren(@Param("parentId") Long parentId,
                               @Param("bookId") Long bookId);

    /**
     * 获取最近编辑的文档
     */
    @Select("""
        SELECT * FROM note_item
        WHERE user_id = #{userId}
          AND type = 'DOCUMENT'
          AND deleted = 0
        ORDER BY updated_at DESC
        LIMIT #{limit}
    """)
    List<NoteItem> getRecentDocuments(@Param("userId") Long userId,
                                      @Param("limit") int limit);

    /**
     * 获取置顶文档
     */
    @Select("""
        SELECT * FROM note_item
        WHERE user_id = #{userId}
          AND type = 'DOCUMENT'
          AND is_pinned = true
          AND deleted = 0
        ORDER BY updated_at DESC
    """)
    List<NoteItem> getPinnedDocuments(@Param("userId") Long userId);
}
