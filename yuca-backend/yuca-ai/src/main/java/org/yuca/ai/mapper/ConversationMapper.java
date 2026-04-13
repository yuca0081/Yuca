package org.yuca.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.yuca.ai.entity.Conversation;

import java.util.List;

/**
 * AI对话记录Mapper
 */
@Mapper
public interface ConversationMapper extends BaseMapper<Conversation> {

    /**
     * 根据session_id查询所有对话记录
     */
    @Select("SELECT * FROM ai_conversation WHERE session_id = #{sessionId} AND deleted = 0 ORDER BY created_at ASC")
    List<Conversation> selectBySessionId(@Param("sessionId") String sessionId);

    /**
     * 根据session_id删除所有对话记录
     */
    int deleteBySessionId(@Param("sessionId") String sessionId);
}
