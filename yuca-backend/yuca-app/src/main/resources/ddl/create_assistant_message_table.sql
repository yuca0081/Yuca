-- =====================================================
-- 建表语句：assistant_message表
-- 创建时间: 2025-01-27
-- 说明：AI助手消息表，存储用户与AI助手的对话记录和token使用统计
-- =====================================================

CREATE TABLE IF NOT EXISTS assistant_message (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL,
    role VARCHAR(50) NOT NULL,
    content TEXT NOT NULL,
    thinking_content TEXT,
    input_tokens INTEGER DEFAULT 0,
    output_tokens INTEGER DEFAULT 0,
    prompt_tokens_details JSONB,
    total_tokens INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_assistant_message_session
        FOREIGN KEY (session_id)
        REFERENCES assistant_session(id)
        ON DELETE CASCADE
);

-- 添加字段注释
COMMENT ON TABLE assistant_message IS 'AI助手消息表，存储用户与AI助手的对话记录';
COMMENT ON COLUMN assistant_message.id IS '主键ID';
COMMENT ON COLUMN assistant_message.session_id IS '会话ID，关联assistant_session表';
COMMENT ON COLUMN assistant_message.role IS '角色：user/assistant/system';
COMMENT ON COLUMN assistant_message.content IS '消息内容';
COMMENT ON COLUMN assistant_message.thinking_content IS 'AI深度思考内容（可选）';
COMMENT ON COLUMN assistant_message.input_tokens IS '输入token数（prompt_tokens）';
COMMENT ON COLUMN assistant_message.output_tokens IS '输出token数（completion_tokens）';
COMMENT ON COLUMN assistant_message.prompt_tokens_details IS '输入token详细信息（JSONB格式），包含：cached_tokens, audio_tokens, text_tokens, image_tokens, video_tokens';
COMMENT ON COLUMN assistant_message.total_tokens IS '总token数（input + output）';
COMMENT ON COLUMN assistant_message.created_at IS '消息创建时间';

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_assistant_message_session_id
ON assistant_message(session_id);

CREATE INDEX IF NOT EXISTS idx_assistant_message_created_at
ON assistant_message(created_at DESC);

CREATE INDEX IF NOT EXISTS idx_assistant_message_role
ON assistant_message(role);

-- 为JSONB字段添加GIN索引（可选，用于JSON查询）
CREATE INDEX IF NOT EXISTS idx_assistant_message_prompt_tokens_details
ON assistant_message USING gin(prompt_tokens_details);

-- =====================================================
-- assistant_session表（如果不存在）
-- =====================================================
CREATE TABLE IF NOT EXISTS assistant_session (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(255),
    model VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE assistant_session IS 'AI助手会话表';
COMMENT ON COLUMN assistant_session.id IS '主键ID';
COMMENT ON COLUMN assistant_session.user_id IS '用户ID';
COMMENT ON COLUMN assistant_session.title IS '会话标题';
COMMENT ON COLUMN assistant_session.model IS '使用的模型名称';
COMMENT ON COLUMN assistant_session.created_at IS '会话创建时间';
COMMENT ON COLUMN assistant_session.updated_at IS '会话更新时间';

CREATE INDEX IF NOT EXISTS idx_assistant_session_user_id
ON assistant_session(user_id);

CREATE INDEX IF NOT EXISTS idx_assistant_session_created_at
ON assistant_session(created_at DESC);
