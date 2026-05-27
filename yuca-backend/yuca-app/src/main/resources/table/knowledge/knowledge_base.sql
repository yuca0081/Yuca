-- ============================================
-- 知识库表
-- ============================================
CREATE TABLE IF NOT EXISTS knowledge_base (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT DEFAULT 0
);

COMMENT ON TABLE knowledge_base IS '知识库表';
COMMENT ON COLUMN knowledge_base.user_id IS '所属用户ID，关联user表';
COMMENT ON COLUMN knowledge_base.name IS '知识库名称';
COMMENT ON COLUMN knowledge_base.description IS '知识库描述';
COMMENT ON COLUMN knowledge_base.deleted IS '逻辑删除标记：0-未删除，1-已删除';

CREATE INDEX IF NOT EXISTS idx_kb_user ON knowledge_base(user_id);
CREATE INDEX IF NOT EXISTS idx_kb_deleted ON knowledge_base(deleted);
