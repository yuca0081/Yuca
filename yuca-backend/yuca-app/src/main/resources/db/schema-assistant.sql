-- Yuca 小助手模块 - 数据库表结构
-- 创建日期: 2025-01-27

-- 会话表
CREATE TABLE IF NOT EXISTS assistant_session (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL,               -- 用户ID，关联user表
    title           VARCHAR(255),                  -- 会话标题（AI自动生成）
    created_at      TIMESTAMP DEFAULT NOW(),       -- 创建时间
    updated_at      TIMESTAMP DEFAULT NOW(),       -- 最后更新时间
    deleted         INTEGER DEFAULT 0              -- 软删除标记: 1-已删除, 0-未删除
);

-- 索引
CREATE INDEX IF NOT EXISTS idx_assistant_session_user ON assistant_session(user_id);
CREATE INDEX IF NOT EXISTS idx_assistant_session_created ON assistant_session(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_assistant_session_deleted ON assistant_session(deleted);

-- 注释
COMMENT ON TABLE assistant_session IS 'AI助手会话表';
COMMENT ON COLUMN assistant_session.title IS '会话标题，首次对话后由AI生成';

-- 消息表
CREATE TABLE IF NOT EXISTS assistant_message (
    id              BIGSERIAL PRIMARY KEY,
    session_id      BIGINT NOT NULL,               -- 会话ID
    role            VARCHAR(20) NOT NULL,          -- 角色：user/assistant/system
    content         TEXT NOT NULL,                 -- 消息内容
    model_name      VARCHAR(50),                   -- 使用的模型名称（仅assistant角色消息有值）
    created_at      TIMESTAMP DEFAULT NOW()        -- 创建时间
);

-- 外键约束
ALTER TABLE assistant_message
    DROP CONSTRAINT IF EXISTS fk_message_session;

ALTER TABLE assistant_message
    ADD CONSTRAINT fk_message_session
    FOREIGN KEY (session_id)
    REFERENCES assistant_session(id)
    ON DELETE CASCADE;

-- 索引
CREATE INDEX IF NOT EXISTS idx_assistant_message_session ON assistant_message(session_id);
CREATE INDEX IF NOT EXISTS idx_assistant_message_created ON assistant_message(created_at);

-- 注释
COMMENT ON TABLE assistant_message IS 'AI助手消息表';
COMMENT ON COLUMN assistant_message.role IS '角色类型：user-用户消息, assistant-AI回复, system-系统提示词';
