-- ============================================
-- 节点表（单表设计：文件夹和文档统一存储）
-- ============================================
CREATE TABLE IF NOT EXISTS note_item (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    book_id         BIGINT NOT NULL,
    parent_id       BIGINT,
    type            VARCHAR(20) NOT NULL,

    -- 通用字段（文件夹和文档都有）
    title           VARCHAR(200) NOT NULL,
    icon            VARCHAR(50),
    sort_order      INT DEFAULT 0,
    is_pinned       BOOLEAN DEFAULT FALSE,

    -- 文档专用字段（FOLDER类型时为NULL）
    content         TEXT,
    content_type    VARCHAR(20),
    summary         VARCHAR(500),
    status          VARCHAR(20),
    view_count      INT,
    word_count      INT,

    -- 文件夹统计字段（DOCUMENT类型时为NULL）
    child_count     INT DEFAULT 0,

    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    published_at    TIMESTAMP,
    deleted         INT DEFAULT 0
);

COMMENT ON TABLE note_item IS '节点表（单表设计：文件夹和文档统一存储）';
COMMENT ON COLUMN note_item.user_id IS '所属用户ID';
COMMENT ON COLUMN note_item.book_id IS '所属笔记本ID';
COMMENT ON COLUMN note_item.parent_id IS '父节点ID（NULL表示笔记本根目录）';
COMMENT ON COLUMN note_item.type IS '节点类型：FOLDER-文件夹，DOCUMENT-文档';
COMMENT ON COLUMN note_item.title IS '标题';
COMMENT ON COLUMN note_item.icon IS '图标';
COMMENT ON COLUMN note_item.sort_order IS '同级排序序号';
COMMENT ON COLUMN note_item.is_pinned IS '是否置顶';
COMMENT ON COLUMN note_item.content IS '文档内容（仅DOCUMENT类型）';
COMMENT ON COLUMN note_item.content_type IS '内容类型：MARKDOWN-Markdown，RICH_TEXT-富文本';
COMMENT ON COLUMN note_item.summary IS '摘要（前500字）';
COMMENT ON COLUMN note_item.status IS '状态：DRAFT-草稿，PUBLISHED-已发布，ARCHIVED-已归档';
COMMENT ON COLUMN note_item.view_count IS '浏览次数';
COMMENT ON COLUMN note_item.word_count IS '字数统计';
COMMENT ON COLUMN note_item.child_count IS '直接子项数量（仅FOLDER类型）';
COMMENT ON COLUMN note_item.published_at IS '发布时间';
COMMENT ON COLUMN note_item.deleted IS '逻辑删除标记：0-正常，1-已删除';

-- 通用索引
CREATE INDEX idx_note_item_user ON note_item(user_id);
CREATE INDEX idx_note_item_book ON note_item(book_id);
CREATE INDEX idx_note_item_parent ON note_item(parent_id);
CREATE INDEX idx_note_item_type ON note_item(type);
CREATE INDEX idx_note_item_deleted ON note_item(deleted);

-- 文档专用部分索引（仅索引文档类型记录）
CREATE INDEX idx_note_item_document_search ON note_item USING gin(
    to_tsvector('chinese', title || ' ' || COALESCE(content, ''))
) WHERE type = 'DOCUMENT';

CREATE INDEX idx_note_item_status ON note_item(status) WHERE type = 'DOCUMENT';
CREATE INDEX idx_note_item_updated ON note_item(updated_at DESC) WHERE type = 'DOCUMENT';
CREATE INDEX idx_note_item_pinned ON note_item(is_pinned, updated_at DESC) WHERE type = 'DOCUMENT' AND is_pinned = true;
