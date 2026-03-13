-- =====================================================
-- Yuca 小助手模块 - 数据库初始化脚本
-- 创建时间: 2025-02-27
-- =====================================================

-- =====================================================
-- 1. 会话表 (assistant_session)
-- =====================================================
CREATE TABLE IF NOT EXISTS assistant_session (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL,                   -- 用户ID，关联user_user表
    title           VARCHAR(255),                      -- 会话标题（首次对话后AI生成）
    model_name      VARCHAR(50) DEFAULT 'qwen3.5-plus',-- 使用的模型名称
    created_at      TIMESTAMP DEFAULT NOW(),           -- 创建时间
    updated_at      TIMESTAMP DEFAULT NOW(),           -- 最后更新时间
    deleted         INTEGER DEFAULT 0                  -- 软删除标记（0-正常，1-已删除）
);

-- 会话表索引
CREATE INDEX IF NOT EXISTS idx_assistant_session_user ON assistant_session(user_id);
CREATE INDEX IF NOT EXISTS idx_assistant_session_updated ON assistant_session(updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_assistant_session_deleted ON assistant_session(deleted);

-- 会话表注释
COMMENT ON TABLE assistant_session IS 'AI助手会话表';
COMMENT ON COLUMN assistant_session.id IS '主键ID';
COMMENT ON COLUMN assistant_session.user_id IS '用户ID，关联user_user表';
COMMENT ON COLUMN assistant_session.title IS '会话标题，首次对话后由AI自动生成';
COMMENT ON COLUMN assistant_session.model_name IS '使用的模型：qwen3.5-plus/qwen-max等';
COMMENT ON COLUMN assistant_session.created_at IS '创建时间';
COMMENT ON COLUMN assistant_session.updated_at IS '最后更新时间';
COMMENT ON COLUMN assistant_session.deleted IS '软删除标记：0-正常，1-已删除';

-- =====================================================
-- 2. 消息表 (assistant_message)
-- =====================================================
CREATE TABLE IF NOT EXISTS assistant_message (
    id              BIGSERIAL PRIMARY KEY,
    session_id      BIGINT NOT NULL,                   -- 会话ID，关联assistant_session表
    role            VARCHAR(20) NOT NULL,              -- 角色：user/assistant/system
    content         TEXT NOT NULL,                     -- 消息内容
    created_at      TIMESTAMP DEFAULT NOW(),           -- 创建时间
    deleted         INTEGER DEFAULT 0                  -- 软删除标记（0-正常，1-已删除）
);

-- 消息表索引
CREATE INDEX IF NOT EXISTS idx_assistant_message_session ON assistant_message(session_id);
CREATE INDEX IF NOT EXISTS idx_assistant_message_created ON assistant_message(created_at);
CREATE INDEX IF NOT EXISTS idx_assistant_message_deleted ON assistant_message(deleted);

-- 消息表注释
COMMENT ON TABLE assistant_message IS 'AI助手消息表';
COMMENT ON COLUMN assistant_message.id IS '主键ID';
COMMENT ON COLUMN assistant_message.session_id IS '会话ID，关联assistant_session表';
COMMENT ON COLUMN assistant_message.role IS '角色类型：user-用户消息, assistant-AI回复, system-系统提示词';
COMMENT ON COLUMN assistant_message.content IS '消息内容，支持长文本';
COMMENT ON COLUMN assistant_message.created_at IS '创建时间';
COMMENT ON COLUMN assistant_message.deleted IS '软删除标记：0-正常，1-已删除';

-- =====================================================
-- 3. 初始化数据（可选）
-- =====================================================

-- 示例：为现有用户创建测试会话（开发环境）
-- INSERT INTO assistant_session (user_id, title, model_name)
-- VALUES (1, '测试对话', 'qwen3.5-plus')
-- ON CONFLICT DO NOTHING;

-- =====================================================
-- 注意事项：
-- 1. 不使用外键约束，在应用层维护数据完整性
-- 2. 使用逻辑删除（deleted字段），物理删除需定期清理
-- 3. session_id和user_id的关联在Service层校验
-- 4. 时间戳使用数据库默认值NOW()，应用层也可显式设置
-- =====================================================
