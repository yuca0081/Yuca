package org.yuca.assistant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.yuca.assistant.entity.AssistantSession;

/**
 * AI助手会话 Mapper
 *
 * @author Yuca
 * @since 2025-01-27
 */
@Mapper
public interface AssistantSessionMapper extends BaseMapper<AssistantSession> {
}
