-- 笔记模块数据库表设计（单表设计）
-- 创建时间：2025-01-10
-- 说明：采用单表设计，文件夹和文档统一存储在 note_item 表中

-- ============================================
-- 1. 笔记本表
-- ============================================
CREATE TABLE IF NOT EXISTS note_book (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL,                 -- 所属用户
    name            VARCHAR(100) NOT NULL,           -- 笔记本名称
    description     TEXT,                            -- 描述
    icon            VARCHAR(50),                     -- 图标
    sort_order      INT DEFAULT 0,                   -- 排序
    is_default      BOOLEAN DEFAULT FALSE,           -- 是否默认笔记本
    color           VARCHAR(20),                     -- 主题颜色
    item_count      INT DEFAULT 0,                   -- 节点数量（文件夹+文档）
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted         INT DEFAULT 0,                   -- 逻辑删除
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


-- ============================================
-- 2. 节点表（单表设计：文件夹和文档统一存储）
-- ============================================
CREATE TABLE IF NOT EXISTS note_item (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL,                 -- 所属用户
    book_id         BIGINT NOT NULL,                 -- 所属笔记本
    parent_id       BIGINT,                          -- 父节点ID（NULL表示笔记本根目录）
    type            VARCHAR(20) NOT NULL,            -- 节点类型：FOLDER, DOCUMENT

    -- 通用字段（文件夹和文档都有）
    title           VARCHAR(200) NOT NULL,           -- 标题
    icon            VARCHAR(50),                     -- 图标
    sort_order      INT DEFAULT 0,                   -- 同级排序
    is_pinned       BOOLEAN DEFAULT FALSE,           -- 是否置顶

    -- 文档专用字段（FOLDER类型时为NULL）
    content         TEXT,                            -- 文档内容
    content_type    VARCHAR(20),                     -- 内容类型：MARKDOWN, RICH_TEXT
    summary         VARCHAR(500),                    -- 摘要（前500字）
    status          VARCHAR(20),                     -- 状态：DRAFT, PUBLISHED, ARCHIVED
    view_count      INT,                             -- 浏览次数
    word_count      INT,                             -- 字数统计

    -- 文件夹统计字段（DOCUMENT类型时为NULL）
    child_count     INT DEFAULT 0,                   -- 直接子项数量

    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    published_at    TIMESTAMP,                       -- 发布时间
    deleted         INT DEFAULT 0,                   -- 逻辑删除

    -- 约束：文件夹类型时content必须为NULL
    CONSTRAINT chk_note_item_folder_content CHECK (
        (type = 'FOLDER' AND content IS NULL AND content_type IS NULL) OR
        (type = 'DOCUMENT')
    ),
    -- 约束：type必须是有效值
    CONSTRAINT chk_note_item_type CHECK (type IN ('FOLDER', 'DOCUMENT'))
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


-- ============================================
-- 3. 标签表
-- ============================================
CREATE TABLE IF NOT EXISTS note_tag (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL,                 -- 所属用户
    name            VARCHAR(50) NOT NULL,            -- 标签名称
    color           VARCHAR(20),                     -- 标签颜色
    use_count       INT DEFAULT 0,                   -- 使用次数
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted         INT DEFAULT 0,                   -- 逻辑删除
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


-- ============================================
-- 4. 节点标签关联表
-- ============================================
CREATE TABLE IF NOT EXISTS note_item_tag (
    id              BIGSERIAL PRIMARY KEY,
    item_id         BIGINT NOT NULL,                 -- 节点ID（仅文档类型）
    tag_id          BIGINT NOT NULL,                 -- 标签ID
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_note_item_tag_item_tag UNIQUE (item_id, tag_id)
);

COMMENT ON TABLE note_item_tag IS '节点标签关联表';
COMMENT ON COLUMN note_item_tag.item_id IS '节点ID（仅文档类型）';
COMMENT ON COLUMN note_item_tag.tag_id IS '标签ID';

CREATE INDEX idx_note_item_tag_item ON note_item_tag(item_id);
CREATE INDEX idx_note_item_tag_tag ON note_item_tag(tag_id);


-- ============================================
-- 5. 版本历史表
-- ============================================
CREATE TABLE IF NOT EXISTS note_version (
    id              BIGSERIAL PRIMARY KEY,
    item_id         BIGINT NOT NULL,                 -- 节点ID（仅文档类型）
    version_number  INT NOT NULL,                    -- 版本号
    title           VARCHAR(200),                    -- 标题
    content         TEXT,                            -- 内容
    change_note     VARCHAR(200),                    -- 变更说明
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


-- ============================================
-- 6. 回收站表
-- ============================================
CREATE TABLE IF NOT EXISTS note_recycle_bin (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL,                 -- 所属用户
    item_type       VARCHAR(20) NOT NULL,            -- 类型：BOOK, FOLDER, DOCUMENT
    item_id         BIGINT NOT NULL,                 -- 原始ID
    item_name       VARCHAR(200) NOT NULL,           -- 名称（用于展示）
    parent_id       BIGINT,                          -- 原父级ID
    extra_data      JSONB,                          -- 额外数据（用于恢复）
    deleted_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expire_at       TIMESTAMP,                       -- 过期时间（30天后自动删除）
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
