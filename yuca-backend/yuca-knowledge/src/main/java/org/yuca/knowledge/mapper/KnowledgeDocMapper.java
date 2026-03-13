package org.yuca.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.yuca.knowledge.entity.KnowledgeDoc;

/**
 * 知识库文档Mapper
 *
 * @author Yuca
 * @since 2025-01-27
 */
@Mapper
public interface KnowledgeDocMapper extends BaseMapper<KnowledgeDoc> {
}
