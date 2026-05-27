-- ============================================
-- 回收站表
-- ============================================
CREATE TABLE IF NOT EXISTS note_recycle_bin (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    item_type       VARCHAR(20) NOT NULL,
    item_id         BIGINT NOT NULL,
    item_name       VARCHAR(200) NOT NULL,
    parent_id       BIGINT,
    extra_data      JSONB,
    deleted_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expire_at       TIMESTAMP,
    CONSTRAINT chk_note_recycle_bin_item_type CHECK (item_type IN ('BOOK', 'FOLDER', 'DOCUMENT'))
);

COMMENT ON TABLE note_recycle_bin IS '回收站表';
COMMENT ON COLUMN note_recycle_bin.user_id IS '所属用户ID';
COMMENT ON COLUMN note_recycle_bin.item_type IS '类型：BOOK-笔记本，FOLDER-文件夹，DOCUMENT-文档';
COMMENT ON COLUMN note_recycle_bin.item_id IS '原始ID';
COMMENT ON COLUMN note_recycle_bin.item_name IS '名称（用于展示）';
COMMENT ON COLUMN note_recycle_bin.parent_id IS '原父级ID';
COMMENT ON COLUMN note_recycle_bin.extra_data IS '额外数据（JSON格式，用于恢复）';
COMMENT ON COLUMN note_recycle_bin.deleted_at IS '删除时间';
COMMENT ON COLUMN note_recycle_bin.expire_at IS '过期时间（30天后自动删除）';

CREATE INDEX idx_note_recycle_bin_user ON note_recycle_bin(user_id);
CREATE INDEX idx_note_recycle_bin_expire ON note_recycle_bin(expire_at);
CREATE INDEX idx_note_recycle_bin_item_type ON note_recycle_bin(item_type);
