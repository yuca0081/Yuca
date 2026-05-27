-- ============================================
-- 文档版本历史表
-- ============================================
CREATE TABLE IF NOT EXISTS note_version (
    id              BIGSERIAL PRIMARY KEY,
    item_id         BIGINT NOT NULL,
    version_number  INT NOT NULL,
    title           VARCHAR(200),
    content         TEXT,
    change_note     VARCHAR(200),
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE note_version IS '文档版本历史表';
COMMENT ON COLUMN note_version.item_id IS '节点ID（仅文档类型）';
COMMENT ON COLUMN note_version.version_number IS '版本号';
COMMENT ON COLUMN note_version.title IS '标题';
COMMENT ON COLUMN note_version.content IS '内容';
COMMENT ON COLUMN note_version.change_note IS '变更说明';

CREATE INDEX idx_note_version_item ON note_version(item_id);
CREATE UNIQUE INDEX idx_note_version_number ON note_version(item_id, version_number);
