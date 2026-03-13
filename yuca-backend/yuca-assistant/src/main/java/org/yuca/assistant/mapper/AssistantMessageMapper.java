package org.yuca.assistant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.yuca.assistant.entity.AssistantMessage;

/**
 * AI助手消息 Mapper
 *
 * @author Yuca
 * @since 2025-01-27
 */
@Mapper
public interface AssistantMessageMapper extends BaseMapper<AssistantMessage> {
}
