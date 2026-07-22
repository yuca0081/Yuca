-- ============================================
-- 查询扩展词汇表（#8 查询扩展：基于 Embedding 的同义词自动发现）
--
-- 用途：检索时从词汇表中按 embedding 相似度找 query 的 top-K 近义词，
--      拼成 "原query OR 同义词1 OR 同义词2" 喂给 BM25 路（websearch_to_tsquery 支持 OR）。
--      向量检索路保持原 query 不变（语义捕获已足够）。
--
-- 词汇来源：
--   1. 自动抽取（source='extracted', doc_id 非 NULL）：上传 md 文档时，
--      把所有 headingLevel > 0 的章节标题批量 embed 入库。
--   2. 管理员预设（source='manual', doc_id = NULL）：通过 SQL 手动塞入领域术语。
--
-- 级联清理：
--   - 文档删除：soft delete WHERE doc_id = ?
--   - 文档重建（增量更新）：先 soft delete 旧 doc_id 的词汇，再抽取新词汇
-- ============================================
DROP TABLE IF EXISTS knowledge_vocabulary;

CREATE TABLE knowledge_vocabulary (
    id            BIGSERIAL PRIMARY KEY,
    kb_id         BIGINT      NOT NULL,
    doc_id        BIGINT,                          -- NULL=管理员预设；非NULL=从文档抽取（便于级联清理）
    term          VARCHAR(500) NOT NULL,           -- 词汇文本，通常是章节标题或领域术语
    embedding     vector(1024),                    -- 词汇向量，1024 维（同 knowledge_chunk）
    source        VARCHAR(50) DEFAULT 'extracted', -- extracted | manual
    created_at    TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    deleted       SMALLINT    DEFAULT 0
);

COMMENT ON TABLE  knowledge_vocabulary             IS '查询扩展词汇表（#8：同义词发现）';
COMMENT ON COLUMN knowledge_vocabulary.kb_id       IS '所属知识库ID';
COMMENT ON COLUMN knowledge_vocabulary.doc_id      IS '所属文档ID；NULL=管理员预设，非NULL=自动抽取（级联清理用）';
COMMENT ON COLUMN knowledge_vocabulary.term        IS '词汇文本，通常是章节标题或领域术语';
COMMENT ON COLUMN knowledge_vocabulary.embedding   IS '词汇向量，1024维，使用pgvector类型';
COMMENT ON COLUMN knowledge_vocabulary.source      IS 'extracted=自动抽取；manual=管理员预设';
COMMENT ON COLUMN knowledge_vocabulary.deleted     IS '逻辑删除标记：0-未删除，1-已删除';

-- HNSW 向量索引（余弦距离，与 knowledge_chunk 同构）
CREATE INDEX idx_vocab_embedding_hnsw
    ON knowledge_vocabulary
    USING hnsw (embedding vector_cosine_ops) WITH (m = 16, ef_construction = 64)
    WHERE deleted = 0;

CREATE INDEX idx_vocab_kb      ON knowledge_vocabulary(kb_id);
CREATE INDEX idx_vocab_doc     ON knowledge_vocabulary(doc_id);
CREATE INDEX idx_vocab_deleted ON knowledge_vocabulary(deleted);
