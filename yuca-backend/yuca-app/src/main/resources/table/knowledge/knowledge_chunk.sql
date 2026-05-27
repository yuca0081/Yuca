-- ============================================
-- 文档切片表
-- ============================================
CREATE TABLE IF NOT EXISTS knowledge_chunk (
    id BIGSERIAL PRIMARY KEY,
    doc_id BIGINT NOT NULL,
    kb_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    embedding vector(1024),
    chunk_index INTEGER NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT DEFAULT 0
);

COMMENT ON TABLE knowledge_chunk IS '文档切片表';
COMMENT ON COLUMN knowledge_chunk.doc_id IS '所属文档ID，关联knowledge_doc表';
COMMENT ON COLUMN knowledge_chunk.kb_id IS '所属知识库ID，关联knowledge_base表（冗余字段，用于优化查询性能）';
COMMENT ON COLUMN knowledge_chunk.content IS '切片文本内容';
COMMENT ON COLUMN knowledge_chunk.embedding IS '向量嵌入，1024维数组，使用pgvector类型';
COMMENT ON COLUMN knowledge_chunk.chunk_index IS '切片序号，表示文档中的顺序';
COMMENT ON COLUMN knowledge_chunk.is_active IS '是否激活：true-启用（参与搜索），false-禁用';
COMMENT ON COLUMN knowledge_chunk.deleted IS '逻辑删除标记：0-未删除，1-已删除';

-- HNSW向量索引（余弦距离）
CREATE INDEX IF NOT EXISTS idx_chunk_embedding_hnsw
    ON knowledge_chunk
    USING hnsw (embedding vector_cosine_ops) WITH (m = 16, ef_construction = 64)
    WHERE deleted = 0 AND is_active = TRUE;

CREATE INDEX IF NOT EXISTS idx_chunk_doc ON knowledge_chunk(doc_id);
CREATE INDEX IF NOT EXISTS idx_chunk_kb ON knowledge_chunk(kb_id);
CREATE INDEX IF NOT EXISTS idx_chunk_active ON knowledge_chunk(is_active);
CREATE INDEX IF NOT EXISTS idx_chunk_deleted ON knowledge_chunk(deleted);

-- RAG增强：中文全文搜索支持
-- 需先启用 zhparser 中文分词扩展
-- CREATE EXTENSION IF NOT EXISTS zhparser;
-- 创建中文全文搜索配置
-- DO $$
-- BEGIN
--     IF NOT EXISTS (SELECT 1 FROM pg_ts_config WHERE cfgname = 'chinese') THEN
--         CREATE TEXT SEARCH CONFIGURATION chinese (PARSER = zhparser);
--         ALTER TEXT SEARCH CONFIGURATION chinese ADD MAPPING FOR n,v,a,i,e,l WITH simple;
--     END IF;
-- END
-- $$;

-- 添加 tsvector 列（使用中文分词配置）
-- ALTER TABLE knowledge_chunk
--     ADD COLUMN IF NOT EXISTS fts_tokens tsvector
--     GENERATED ALWAYS AS (to_tsvector('chinese', content)) STORED;

-- GIN 索引加速全文搜索
-- CREATE INDEX IF NOT EXISTS idx_chunk_fts
--     ON knowledge_chunk USING gin(fts_tokens)
--     WHERE deleted = 0 AND is_active = TRUE;
