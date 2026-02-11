package org.yuca.yuca.note.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.yuca.yuca.note.entity.NoteTag;

/**
 * 标签Mapper接口
 */
@Mapper
public interface NoteTagMapper extends BaseMapper<NoteTag> {
}
