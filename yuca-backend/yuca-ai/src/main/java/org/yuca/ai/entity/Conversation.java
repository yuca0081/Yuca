package org.yuca.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.yuca.ai.memory.JsonbTypeHandler;

import java.time.LocalDateTime;

/**
 * AI对话记录实体
 */
@Data
@TableName("ai_conversation")
public class Conversation {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 会话ID，用于区分不同的对话会话
     */
    private String sessionId;

    /**
     * 消息类型：USER或AI
     */
    private String messageType;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 工具调用信息，JSON格式
     * 使用自定义TypeHandler处理PostgreSQL的jsonb类型
     */
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String toolCalls;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 逻辑删除标志
     */
    @TableLogic
    private Integer deleted;
}
