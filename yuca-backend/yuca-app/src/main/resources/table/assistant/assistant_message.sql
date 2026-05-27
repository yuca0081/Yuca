-- ============================================
-- AI助手消息表
-- ============================================
CREATE TABLE IF NOT EXISTS assistant_message (
    id              BIGSERIAL PRIMARY KEY,
    session_id      BIGINT NOT NULL,
    role            VARCHAR(20) NOT NULL,
    content         TEXT NOT NULL,
    model_name      VARCHAR(50),
    thinking_content TEXT,
    input_tokens    INTEGER DEFAULT 0,
    output_tokens   INTEGER DEFAULT 0,
    prompt_tokens_details TEXT,
    total_tokens    INTEGER DEFAULT 0,
    created_at      TIMESTAMP DEFAULT NOW()
);

COMMENT ON TABLE assistant_message IS 'AI助手消息表';
COMMENT ON COLUMN assistant_message.id IS '主键ID';
COMMENT ON COLUMN assistant_message.session_id IS '会话ID，关联assistant_session表';
COMMENT ON COLUMN assistant_message.role IS '角色类型：user-用户消息, assistant-AI回复, system-系统提示词';
COMMENT ON COLUMN assistant_message.content IS '消息内容，支持长文本';
COMMENT ON COLUMN assistant_message.model_name IS '使用的模型：qwen-plus/qwen-max等（仅assistant角色消息有值）';
COMMENT ON COLUMN assistant_message.thinking_content IS 'AI深度思考内容，当启用深度思考模式时保存';
COMMENT ON COLUMN assistant_message.input_tokens IS '输入token数（prompt_tokens）';
COMMENT ON COLUMN assistant_message.output_tokens IS '输出token数（completion_tokens，包含思考token）';
COMMENT ON COLUMN assistant_message.prompt_tokens_details IS '输入token详细信息（JSON格式），包含cached_tokens, audio_tokens等';
COMMENT ON COLUMN assistant_message.total_tokens IS '总token数（input + output）';
COMMENT ON COLUMN assistant_message.created_at IS '创建时间';

CREATE INDEX IF NOT EXISTS idx_assistant_message_session ON assistant_message(session_id);
CREATE INDEX IF NOT EXISTS idx_assistant_message_created ON assistant_message(created_at);
CREATE INDEX IF NOT EXISTS idx_assistant_message_tokens ON assistant_message(total_tokens DESC)
    WHERE total_tokens > 0;
