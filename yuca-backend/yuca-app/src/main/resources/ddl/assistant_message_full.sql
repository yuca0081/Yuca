-- =====================================================
-- Yuca 小助手模块 - assistant_message 完整建表语句
-- 创建时间: 2025-01-27
-- 说明：包含深度思考内容和token统计字段
-- =====================================================

CREATE TABLE IF NOT EXISTS assistant_message (
    id              BIGSERIAL PRIMARY KEY,
    session_id      BIGINT NOT NULL,                      -- 会话ID，关联assistant_session表
    role            VARCHAR(20) NOT NULL,                 -- 角色：user/assistant/system
    content         TEXT NOT NULL,                        -- 消息内容
    thinking_content TEXT,                               -- 深度思考内容（可选，深度思考模式时保存）
    input_tokens    INTEGER DEFAULT 0,                   -- 输入token数（prompt_tokens）
    output_tokens   INTEGER DEFAULT 0,                   -- 输出token数（completion_tokens）
    total_tokens    INTEGER DEFAULT 0,                   -- 总token数（input + output）
    created_at      TIMESTAMP DEFAULT NOW(),             -- 创建时间
    deleted         INTEGER DEFAULT 0                    -- 软删除标记（0-正常，1-已删除）
);

-- 消息表索引
CREATE INDEX IF NOT EXISTS idx_assistant_message_session ON assistant_message(session_id);
CREATE INDEX IF NOT EXISTS idx_assistant_message_created ON assistant_message(created_at);
CREATE INDEX IF NOT EXISTS idx_assistant_message_deleted ON assistant_message(deleted);
CREATE INDEX IF NOT EXISTS idx_assistant_message_tokens ON assistant_message(total_tokens DESC)
    WHERE total_tokens > 0;

-- 消息表注释
COMMENT ON TABLE assistant_message IS 'AI助手消息表';
COMMENT ON COLUMN assistant_message.id IS '主键ID';
COMMENT ON COLUMN assistant_message.session_id IS '会话ID，关联assistant_session表';
COMMENT ON COLUMN assistant_message.role IS '角色类型：user-用户消息, assistant-AI回复, system-系统提示词';
COMMENT ON COLUMN assistant_message.content IS '消息内容，支持长文本';
COMMENT ON COLUMN assistant_message.thinking_content IS 'AI深度思考内容，当启用深度思考模式时保存';
COMMENT ON COLUMN assistant_message.input_tokens IS '输入token数（prompt_tokens）';
COMMENT ON COLUMN assistant_message.output_tokens IS '输出token数（completion_tokens，包含思考token）';
COMMENT ON COLUMN assistant_message.total_tokens IS '总token数（input + output）';
COMMENT ON COLUMN assistant_message.created_at IS '创建时间';
COMMENT ON COLUMN assistant_message.deleted IS '软删除标记：0-正常，1-已删除';

-- =====================================================
-- 如果表已存在，执行以下ALTER语句添加新字段
-- =====================================================

-- ALTER TABLE assistant_message
-- ADD COLUMN IF NOT EXISTS thinking_content TEXT;

-- ALTER TABLE assistant_message
-- ADD COLUMN IF NOT EXISTS input_tokens INTEGER DEFAULT 0;

-- ALTER TABLE assistant_message
-- ADD COLUMN IF NOT EXISTS output_tokens INTEGER DEFAULT 0;

-- ALTER TABLE assistant_message
-- ADD COLUMN IF NOT EXISTS thinking_tokens INTEGER DEFAULT 0;

-- ALTER TABLE assistant_message
-- ADD COLUMN IF NOT EXISTS total_tokens INTEGER DEFAULT 0;

-- COMMENT ON COLUMN assistant_message.thinking_content IS 'AI深度思考内容，当启用深度思考模式时保存';
-- COMMENT ON COLUMN assistant_message.input_tokens IS '输入token数（prompt_tokens）';
-- COMMENT ON COLUMN assistant_message.output_tokens IS '输出token数（completion_tokens）';
-- COMMENT ON COLUMN assistant_message.thinking_tokens IS '深度思考token数';
-- COMMENT ON COLUMN assistant_message.total_tokens IS '总token数（input + output + thinking）';
