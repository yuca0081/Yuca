package org.yuca.yuca.note.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.yuca.yuca.note.entity.NoteBook;

/**
 * 笔记本Mapper接口
 */
@Mapper
public interface NoteBookMapper extends BaseMapper<NoteBook> {
}
