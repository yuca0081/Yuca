package org.yuca.ai.history;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.yuca.infrastructure.handle.JsonbTypeHandler;

import java.time.LocalDateTime;

/**
 * 对话历史记录实体
 */
@Data
@TableName("ai_chat_history")
public class ChatHistory {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 会话ID */
    private String sessionId;

    /** 消息类型：USER、AI、TOOL、TOOL_RESULT、SYSTEM */
    private String messageType;

    /** 消息内容 */
    private String content;

    /** 工具调用信息，JSON格式 */
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String toolCalls;

    /** Token 使用统计，JSON格式 */
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String tokenUsage;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableLogic
    private Integer deleted;
}
