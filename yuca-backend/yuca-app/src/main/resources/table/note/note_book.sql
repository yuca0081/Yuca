-- ============================================
-- 笔记本表
-- ============================================
CREATE TABLE IF NOT EXISTS note_book (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    name            VARCHAR(100) NOT NULL,
    description     TEXT,
    icon            VARCHAR(50),
    sort_order      INT DEFAULT 0,
    is_default      BOOLEAN DEFAULT FALSE,
    color           VARCHAR(20),
    item_count      INT DEFAULT 0,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted         INT DEFAULT 0,
    CONSTRAINT uk_note_book_user_name UNIQUE (user_id, name)
);

COMMENT ON TABLE note_book IS '笔记本表';
COMMENT ON COLUMN note_book.user_id IS '所属用户ID';
COMMENT ON COLUMN note_book.name IS '笔记本名称';
COMMENT ON COLUMN note_book.description IS '笔记本描述';
COMMENT ON COLUMN note_book.icon IS '图标';
COMMENT ON COLUMN note_book.sort_order IS '排序序号';
COMMENT ON COLUMN note_book.is_default IS '是否默认笔记本';
COMMENT ON COLUMN note_book.color IS '主题颜色';
COMMENT ON COLUMN note_book.item_count IS '节点数量（文件夹+文档）';
COMMENT ON COLUMN note_book.deleted IS '逻辑删除标记：0-正常，1-已删除';

CREATE INDEX idx_note_book_user ON note_book(user_id);
CREATE INDEX idx_note_book_deleted ON note_book(deleted);
