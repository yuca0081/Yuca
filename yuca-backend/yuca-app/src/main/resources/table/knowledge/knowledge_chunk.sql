-- ============================================
-- 文档切片表（含章节树字段）
-- 章节树字段（title/heading_level/breadcrumb/parent_id/summary/line_start/line_end）
-- 全部可空：非 md 文件的 chunk 这些字段为 NULL，行为同重构前
-- ============================================
DROP TABLE IF EXISTS knowledge_chunk;

CREATE TABLE knowledge_chunk (
    id            BIGSERIAL PRIMARY KEY,
    doc_id        BIGINT      NOT NULL,
    kb_id         BIGINT      NOT NULL,
    content       TEXT        NOT NULL,
    embedding     vector(1024),
    chunk_index   INTEGER     NOT NULL,
    is_active     BOOLEAN     DEFAULT TRUE,
    created_at    TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    deleted       SMALLINT    DEFAULT 0,

    -- ========== 章节树字段（基于 H1-H6 标题层级的独占式切片） ==========
    title         VARCHAR(500),                       -- 章节标题；非 md 平切片为 NULL
    heading_level SMALLINT,                           -- 标题层级 1-6 对应 H1-H6；NULL 表示非 md 平切片
    breadcrumb    VARCHAR(2000),                      -- 面包屑路径，e.g. "RAG > 召回 > 切块策略"
    parent_id     BIGINT,                             -- 父节点 DB id；NULL 表示根节点
    summary       TEXT,                               -- LLM 摘要（预留，v1 不填）
    line_start    INTEGER,                            -- 源文件起始行号（含，从 1 开始）
    line_end      INTEGER                             -- 源文件结束行号（含）
);

COMMENT ON TABLE  knowledge_chunk              IS '文档切片表';
COMMENT ON COLUMN knowledge_chunk.doc_id       IS '所属文档ID，关联knowledge_doc表';
COMMENT ON COLUMN knowledge_chunk.kb_id        IS '所属知识库ID，关联knowledge_base表（冗余字段，优化查询性能）';
COMMENT ON COLUMN knowledge_chunk.content      IS '切片文本内容（独占式切片：自己标题后到下一标题前的正文，不含子节点正文）';
COMMENT ON COLUMN knowledge_chunk.embedding    IS '向量嵌入，1024维，使用pgvector类型';
COMMENT ON COLUMN knowledge_chunk.chunk_index  IS '切片序号，表示文档中的顺序';
COMMENT ON COLUMN knowledge_chunk.is_active    IS '是否激活：true-启用（参与搜索），false-禁用';
COMMENT ON COLUMN knowledge_chunk.deleted      IS '逻辑删除标记：0-未删除，1-已删除';
COMMENT ON COLUMN knowledge_chunk.title        IS '章节标题；非 md 平切片为 NULL';
COMMENT ON COLUMN knowledge_chunk.heading_level IS '标题层级 1-6 对应 H1-H6；NULL 表示非 md 平切片';
COMMENT ON COLUMN knowledge_chunk.breadcrumb   IS '面包屑路径，e.g. "RAG > 召回 > 切块策略"';
COMMENT ON COLUMN knowledge_chunk.parent_id    IS '父节点 DB id；NULL 表示根节点';
COMMENT ON COLUMN knowledge_chunk.summary      IS 'LLM 摘要（预留，v1 不填）';
COMMENT ON COLUMN knowledge_chunk.line_start   IS '源文件起始行号（含，从 1 开始）';
COMMENT ON COLUMN knowledge_chunk.line_end     IS '源文件结束行号（含）';

-- HNSW 向量索引（余弦距离）
CREATE INDEX idx_chunk_embedding_hnsw
    ON knowledge_chunk
    USING hnsw (embedding vector_cosine_ops) WITH (m = 16, ef_construction = 64)
    WHERE deleted = 0 AND is_active = TRUE;

CREATE INDEX idx_chunk_doc     ON knowledge_chunk(doc_id);
CREATE INDEX idx_chunk_kb      ON knowledge_chunk(kb_id);
CREATE INDEX idx_chunk_active  ON knowledge_chunk(is_active);
CREATE INDEX idx_chunk_deleted ON knowledge_chunk(deleted);
CREATE INDEX idx_chunk_parent  ON knowledge_chunk(parent_id) WHERE deleted = 0;

-- RAG增强：中文全文搜索支持（需先启用 zhparser 中文分词扩展，按需取消注释）
-- CREATE EXTENSION IF NOT EXISTS zhparser;
-- DO $$
-- BEGIN
--     IF NOT EXISTS (SELECT 1 FROM pg_ts_config WHERE cfgname = 'chinese') THEN
--         CREATE TEXT SEARCH CONFIGURATION chinese (PARSER = zhparser);
--         ALTER TEXT SEARCH CONFIGURATION chinese ADD MAPPING FOR n,v,a,i,e,l WITH simple;
--     END IF;
-- END
-- $$;
-- ALTER TABLE knowledge_chunk
--     ADD COLUMN fts_tokens tsvector
--     GENERATED ALWAYS AS (to_tsvector('chinese', content)) STORED;
-- CREATE INDEX idx_chunk_fts
--     ON knowledge_chunk USING gin(fts_tokens)
--     WHERE deleted = 0 AND is_active = TRUE;
