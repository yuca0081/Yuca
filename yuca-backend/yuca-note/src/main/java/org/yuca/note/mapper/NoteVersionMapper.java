package org.yuca.note.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.yuca.note.entity.NoteVersion;

import java.util.List;

/**
 * 文档版本历史Mapper接口
 */
@Mapper
public interface NoteVersionMapper extends BaseMapper<NoteVersion> {

    /**
     * 获取文档的所有版本（按版本号降序）
     */
    @Select("SELECT * FROM note_version WHERE item_id = #{itemId} ORDER BY version_number DESC")
    List<NoteVersion> getVersionsByItemId(@Param("itemId") Long itemId);

    /**
     * 获取文档的指定版本
     */
    @Select("SELECT * FROM note_version WHERE item_id = #{itemId} AND version_number = #{versionNumber}")
    NoteVersion getVersionByNumber(@Param("itemId") Long itemId,
                                    @Param("versionNumber") Integer versionNumber);

    /**
     * 获取文档的最新版本号
     */
    @Select("SELECT COALESCE(MAX(version_number), 0) FROM note_version WHERE item_id = #{itemId}")
    Integer getMaxVersionNumber(@Param("itemId") Long itemId);
}
