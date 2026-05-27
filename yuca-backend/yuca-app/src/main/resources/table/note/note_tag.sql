-- ============================================
-- 标签表
-- ============================================
CREATE TABLE IF NOT EXISTS note_tag (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    name            VARCHAR(50) NOT NULL,
    color           VARCHAR(20),
    use_count       INT DEFAULT 0,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted         INT DEFAULT 0,
    CONSTRAINT uk_note_tag_user_name UNIQUE (user_id, name)
);

COMMENT ON TABLE note_tag IS '标签表';
COMMENT ON COLUMN note_tag.user_id IS '所属用户ID';
COMMENT ON COLUMN note_tag.name IS '标签名称';
COMMENT ON COLUMN note_tag.color IS '标签颜色';
COMMENT ON COLUMN note_tag.use_count IS '使用次数';
COMMENT ON COLUMN note_tag.deleted IS '逻辑删除标记：0-正常，1-已删除';

CREATE INDEX idx_note_tag_user ON note_tag(user_id);
CREATE INDEX idx_note_tag_deleted ON note_tag(deleted);
