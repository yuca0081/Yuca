-- 对话历史表
CREATE TABLE IF NOT EXISTS ai_chat_history (
    id BIGSERIAL PRIMARY KEY,
    session_id VARCHAR(255) NOT NULL,
    message_type VARCHAR(50) NOT NULL, -- 'USER' or 'AI'
    content TEXT NOT NULL,
    tool_calls JSONB, -- 工具调用信息
    token_usage JSONB, -- Token使用统计
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0 -- 逻辑删除标志
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_chat_history_session_id ON ai_chat_history(session_id);
CREATE INDEX IF NOT EXISTS idx_chat_history_created_at ON ai_chat_history(created_at);

-- 评论
COMMENT ON TABLE ai_chat_history IS '对话历史记录表';
COMMENT ON COLUMN ai_chat_history.session_id IS '会话ID，用于区分不同的对话会话';
COMMENT ON COLUMN ai_chat_history.message_type IS '消息类型：USER、AI、TOOL、TOOL_RESULT、SYSTEM';
COMMENT ON COLUMN ai_chat_history.tool_calls IS '工具调用信息，JSON格式';
COMMENT ON COLUMN ai_chat_history.token_usage IS 'Token使用统计，JSON格式，如 {"inputTokenCount":10,"outputTokenCount":20,"totalTokenCount":30}';
