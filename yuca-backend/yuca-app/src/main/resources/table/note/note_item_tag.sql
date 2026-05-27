-- ============================================
-- 节点标签关联表
-- ============================================
CREATE TABLE IF NOT EXISTS note_item_tag (
    id              BIGSERIAL PRIMARY KEY,
    item_id         BIGINT NOT NULL,
    tag_id          BIGINT NOT NULL,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_note_item_tag_item_tag UNIQUE (item_id, tag_id)
);

COMMENT ON TABLE note_item_tag IS '节点标签关联表';
COMMENT ON COLUMN note_item_tag.item_id IS '节点ID（仅文档类型）';
COMMENT ON COLUMN note_item_tag.tag_id IS '标签ID';

CREATE INDEX idx_note_item_tag_item ON note_item_tag(item_id);
CREATE INDEX idx_note_item_tag_tag ON note_item_tag(tag_id);
