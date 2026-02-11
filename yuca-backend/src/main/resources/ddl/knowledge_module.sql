-- =====================================================
-- 2. 知识库表 (knowledge_base)
-- =====================================================
drop table knowledge_base;
CREATE TABLE IF NOT EXISTS knowledge_base (
                                              id BIGSERIAL PRIMARY KEY,
                                              user_id BIGINT NOT NULL,                  -- 所属用户ID
                                              name VARCHAR(100) NOT NULL,               -- 知识库名称
    description TEXT,                         -- 知识库描述
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT DEFAULT 0              -- 逻辑删除标记（0-未删除，1-已删除）
    );

-- 索引
CREATE INDEX IF NOT EXISTS idx_kb_user ON knowledge_base(user_id);
CREATE INDEX IF NOT EXISTS idx_kb_deleted ON knowledge_base(deleted);

-- 注释
COMMENT ON TABLE knowledge_base IS '知识库表';
COMMENT ON COLUMN knowledge_base.user_id IS '所属用户ID，关联user表';
COMMENT ON COLUMN knowledge_base.name IS '知识库名称';
COMMENT ON COLUMN knowledge_base.description IS '知识库描述';
COMMENT ON COLUMN knowledge_base.deleted IS '逻辑删除标记：0-未删除，1-已删除';

-- =====================================================
-- 3. 文档表 (knowledge_doc)
-- =====================================================
CREATE TABLE IF NOT EXISTS knowledge_doc (
                                             id BIGSERIAL PRIMARY KEY,
                                             kb_id BIGINT NOT NULL,                    -- 所属知识库ID
                                             doc_name VARCHAR(255) NOT NULL,           -- 文档名称
    doc_format VARCHAR(20) NOT NULL,          -- 文档格式 (md, txt, pdf, docx)
    doc_size BIGINT,                          -- 文件大小（字节）
    file_path TEXT,                           -- MinIO存储路径
    data_source VARCHAR(100),                 -- 数据来源
    metadata JSONB,                           -- 元数据（JSON格式）
    chunk_count INTEGER DEFAULT 0,            -- 切片数量
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT DEFAULT 0              -- 逻辑删除标记（0-未删除，1-已删除）
    );

-- 索引
CREATE INDEX IF NOT EXISTS idx_doc_kb ON knowledge_doc(kb_id);
CREATE INDEX IF NOT EXISTS idx_doc_deleted ON knowledge_doc(deleted);
CREATE INDEX IF NOT EXISTS idx_doc_format ON knowledge_doc(doc_format);

-- 注释
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

-- =====================================================
-- 4. 文档切片表 (knowledge_chunk)
-- =====================================================
CREATE TABLE IF NOT EXISTS knowledge_chunk (
                                               id BIGSERIAL PRIMARY KEY,
                                               doc_id BIGINT NOT NULL,                   -- 所属文档ID
                                               kb_id BIGINT NOT NULL,                    -- 所属知识库ID（冗余，优化查询）
                                               content TEXT NOT NULL,                    -- 文本内容
                                               embedding vector(1536),                   -- 向量嵌入（1536维，使用pgvector）
    chunk_index INTEGER NOT NULL,             -- 切片序号
    is_active BOOLEAN DEFAULT TRUE,           -- 是否激活（可用于启用/禁用切片）
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT DEFAULT 0              -- 逻辑删除标记（0-未删除，1-已删除）
    );

-- 创建HNSW向量索引（高效近似最近邻搜索）
-- 使用余弦距离
-- 参数说明（可选）：
--   m = 16: 每个节点的连接数（默认16，范围2-100），越大精度越高但占用空间越大
--   ef_construction = 64: 构建时的候选数（默认40，范围4-1000），越大质量越好但构建越慢
CREATE INDEX IF NOT EXISTS idx_chunk_embedding_hnsw
    ON knowledge_chunk
    USING hnsw (embedding vector_cosine_ops) WITH (m = 16, ef_construction = 64)
    WHERE deleted = 0 AND is_active = TRUE;

-- 其他索引
CREATE INDEX IF NOT EXISTS idx_chunk_doc ON knowledge_chunk(doc_id);
CREATE INDEX IF NOT EXISTS idx_chunk_kb ON knowledge_chunk(kb_id);
CREATE INDEX IF NOT EXISTS idx_chunk_active ON knowledge_chunk(is_active);
CREATE INDEX IF NOT EXISTS idx_chunk_deleted ON knowledge_chunk(deleted);

-- 注释
COMMENT ON TABLE knowledge_chunk IS '文档切片表';
COMMENT ON COLUMN knowledge_chunk.doc_id IS '所属文档ID，关联knowledge_doc表';
COMMENT ON COLUMN knowledge_chunk.kb_id IS '所属知识库ID，关联knowledge_base表（冗余字段，用于优化查询性能）';
COMMENT ON COLUMN knowledge_chunk.content IS '切片文本内容';
COMMENT ON COLUMN knowledge_chunk.embedding IS '向量嵌入，1536维数组，使用pgvector类型';
COMMENT ON COLUMN knowledge_chunk.chunk_index IS '切片序号，表示文档中的顺序';
COMMENT ON COLUMN knowledge_chunk.is_active IS '是否激活：true-启用（参与搜索），false-禁用';
COMMENT ON COLUMN knowledge_chunk.deleted IS '逻辑删除标记：0-未删除，1-已删除';