package org.yuca.infrastructure.storage.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.yuca.infrastructure.storage.entity.StorageFile;

/**
 * 文件存储记录Mapper
 *
 * @author Yuca
 * @since 2025-01-29
 */
@Mapper
public interface StorageFileMapper extends BaseMapper<StorageFile> {
}
