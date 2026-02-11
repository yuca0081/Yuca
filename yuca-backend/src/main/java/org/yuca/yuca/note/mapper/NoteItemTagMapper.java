package org.yuca.yuca.note.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.yuca.yuca.note.entity.NoteItemTag;

import java.util.List;

/**
 * 节点标签关联Mapper接口
 */
@Mapper
public interface NoteItemTagMapper extends BaseMapper<NoteItemTag> {

    /**
     * 获取文档的所有标签ID
     */
    @Select("SELECT tag_id FROM note_item_tag WHERE item_id = #{itemId}")
    List<Long> getTagIdsByItemId(@Param("itemId") Long itemId);

    /**
     * 获取标签下的所有文档ID
     */
    @Select("SELECT item_id FROM note_item_tag WHERE tag_id = #{tagId}")
    List<Long> getItemIdsByTagId(@Param("tagId") Long tagId);
}
