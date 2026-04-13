-- 对话表
CREATE TABLE IF NOT EXISTS ai_conversation (
    id BIGSERIAL PRIMARY KEY,
    session_id VARCHAR(255) NOT NULL,
    message_type VARCHAR(50) NOT NULL, -- 'USER' or 'AI'
    content TEXT NOT NULL,
    tool_calls JSONB, -- 工具调用信息
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0 -- 逻辑删除标志
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_conversation_session_id ON ai_conversation(session_id);
CREATE INDEX IF NOT EXISTS idx_conversation_created_at ON ai_conversation(created_at);

-- 工具调用记录表（可选，用于详细记录工具调用）
CREATE TABLE IF NOT EXISTS ai_tool_call (
    id BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT NOT NULL REFERENCES ai_conversation(id) ON DELETE CASCADE,
    tool_name VARCHAR(255) NOT NULL,
    tool_arguments TEXT,
    execution_result TEXT,
    execution_duration_ms BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_tool_call_conversation_id ON ai_tool_call(conversation_id);
CREATE INDEX IF NOT EXISTS idx_tool_call_name ON ai_tool_call(tool_name);

-- 评论：用于记录对话的元数据（可选）
COMMENT ON TABLE ai_conversation IS 'AI对话记录表';
COMMENT ON TABLE ai_tool_call IS 'AI工具调用记录表';
COMMENT ON COLUMN ai_conversation.session_id IS '会话ID，用于区分不同的对话会话';
COMMENT ON COLUMN ai_conversation.message_type IS '消息类型：USER或AI';
COMMENT ON COLUMN ai_conversation.tool_calls IS '工具调用信息，JSON格式';
COMMENT ON COLUMN ai_tool_call.execution_duration_ms IS '工具执行耗时（毫秒）';
