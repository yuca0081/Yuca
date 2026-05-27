-- ============================================
-- AI助手会话表
-- ============================================
CREATE TABLE IF NOT EXISTS assistant_session (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    title           VARCHAR(255),
    created_at      TIMESTAMP DEFAULT NOW(),
    updated_at      TIMESTAMP DEFAULT NOW(),
    deleted         INTEGER DEFAULT 0
);

COMMENT ON TABLE assistant_session IS 'AI助手会话表';
COMMENT ON COLUMN assistant_session.id IS '主键ID';
COMMENT ON COLUMN assistant_session.user_id IS '用户ID，关联user_user表';
COMMENT ON COLUMN assistant_session.title IS '会话标题，首次对话后由AI自动生成';
COMMENT ON COLUMN assistant_session.created_at IS '创建时间';
COMMENT ON COLUMN assistant_session.updated_at IS '最后更新时间';
COMMENT ON COLUMN assistant_session.deleted IS '软删除标记：0-正常，1-已删除';

CREATE INDEX IF NOT EXISTS idx_assistant_session_user ON assistant_session(user_id);
CREATE INDEX IF NOT EXISTS idx_assistant_session_updated ON assistant_session(updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_assistant_session_deleted ON assistant_session(deleted);
