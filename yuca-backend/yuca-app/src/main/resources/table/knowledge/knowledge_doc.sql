-- ============================================
-- 知识库文档表
-- ============================================
CREATE TABLE IF NOT EXISTS knowledge_doc (
    id BIGSERIAL PRIMARY KEY,
    kb_id BIGINT NOT NULL,
    doc_name VARCHAR(255) NOT NULL,
    doc_format VARCHAR(20) NOT NULL,
    doc_size BIGINT,
    file_path TEXT,
    data_source VARCHAR(100),
    metadata JSONB,
    chunk_count INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT DEFAULT 0
);

COMMENT ON TABLE knowledge_doc IS '知识库文档表';
COMMENT ON COLUMN knowledge_doc.kb_id IS '所属知识库ID，关联knowledge_base表';
COMMENT ON COLUMN knowledge_doc.doc_name IS '文档原始文件名';
COMMENT ON COLUMN knowledge_doc.doc_format IS '文档格式：md, txt, pdf, docx等';
COMMENT ON COLUMN knowledge_doc.doc_size IS '文件大小，单位：字节';
COMMENT ON COLUMN knowledge_doc.file_path IS 'MinIO对象存储路径';
COMMENT ON COLUMN knowledge_doc.data_source IS '数据来源标识';
COMMENT ON COLUMN knowledge_doc.metadata IS '文档元数据，JSONB格式存储';
COMMENT ON COLUMN knowledge_doc.chunk_count IS '文档切片数量';
COMMENT ON COLUMN knowledge_doc.deleted IS '逻辑删除标记：0-未删除，1-已删除';

CREATE INDEX IF NOT EXISTS idx_doc_kb ON knowledge_doc(kb_id);
CREATE INDEX IF NOT EXISTS idx_doc_deleted ON knowledge_doc(deleted);
CREATE INDEX IF NOT EXISTS idx_doc_format ON knowledge_doc(doc_format);
