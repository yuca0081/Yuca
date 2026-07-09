-- ============================================
-- 知识库文档表
-- 记录用户上传到知识库的每一份文档元信息（不含正文内容，正文存在切片表 knowledge_chunk）
-- 一条 knowledge_doc 记录对应 N 条 knowledge_chunk 记录（章节树切片或扁平字符切片）
-- ============================================
DROP TABLE IF EXISTS knowledge_doc;

CREATE TABLE knowledge_doc (
    id              BIGSERIAL    PRIMARY KEY,                 -- 主键，自增
    kb_id           BIGINT       NOT NULL,                    -- 所属知识库 ID（关联 knowledge_base.id），用于权限隔离和数据归属
    doc_name        VARCHAR(255) NOT NULL,                    -- 文档原始文件名（如 "RAG系统设计.md"），仅作展示，不参与查重
    doc_format      VARCHAR(20)  NOT NULL,                    -- 文档格式扩展名：md / txt / pdf / docx，决定走章节树切片还是扁平切片
    doc_size        BIGINT,                                   -- 文件字节数，用于前端展示和大小限制校验
    file_path       TEXT,                                    -- MinIO 对象存储路径（如 "knowledge/3/RAG系统设计.md"），下载时拼回 MinIO endpoint
    data_source     VARCHAR(100),                             -- 数据来源标识：当前固定 "upload"，预留接口将来扩展（如 "crawl" / "api_sync"）
    metadata        JSONB,                                   -- 文档元数据（JSON），预留字段，v1 未使用
    chunk_count     INTEGER      DEFAULT 0,                   -- 该文档切片总数（章节树模式下含所有子孙节点），用于前端展示和运营统计
    content_hash    VARCHAR(64)  NOT NULL,                    -- 文件内容 SHA256 指纹（64 位十六进制小写），上传查重的关键——同 kb_id 下命中已存在 hash 直接跳过解析/embedding/插入流程
    created_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,   -- 创建时间
    updated_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,   -- 更新时间
    deleted         SMALLINT     DEFAULT 0                    -- 逻辑删除标记：0-未删除，1-已删除（MyBatis-Plus @TableLogic 自动处理）
);

COMMENT ON TABLE  knowledge_doc              IS '知识库文档表：每条记录对应一份用户上传的文档，正文切片存于 knowledge_chunk';
COMMENT ON COLUMN knowledge_doc.id           IS '主键，自增';
COMMENT ON COLUMN knowledge_doc.kb_id        IS '所属知识库ID，关联knowledge_base表，用于权限隔离';
COMMENT ON COLUMN knowledge_doc.doc_name     IS '文档原始文件名，仅展示用，不参与查重';
COMMENT ON COLUMN knowledge_doc.doc_format   IS '文档格式：md, txt, pdf, docx等，决定切片策略';
COMMENT ON COLUMN knowledge_doc.doc_size     IS '文件大小（字节）';
COMMENT ON COLUMN knowledge_doc.file_path    IS 'MinIO对象存储路径';
COMMENT ON COLUMN knowledge_doc.data_source  IS '数据来源标识：upload / crawl / api_sync 等';
COMMENT ON COLUMN knowledge_doc.metadata     IS '文档元数据，JSONB格式存储（预留）';
COMMENT ON COLUMN knowledge_doc.chunk_count  IS '文档切片数量（章节树模式含所有子孙节点）';
COMMENT ON COLUMN knowledge_doc.content_hash IS '内容 SHA256 指纹（64 位 hex 小写）；上传查重用，同 kb_id 下命中已存在 hash 直接跳过索引';
COMMENT ON COLUMN knowledge_doc.created_at   IS '创建时间';
COMMENT ON COLUMN knowledge_doc.updated_at   IS '更新时间';
COMMENT ON COLUMN knowledge_doc.deleted      IS '逻辑删除标记：0-未删除，1-已删除';

-- ============================================
-- 索引
-- ============================================
CREATE INDEX idx_doc_kb       ON knowledge_doc(kb_id);                          -- 按知识库查文档列表（最常见查询）
CREATE INDEX idx_doc_deleted  ON knowledge_doc(deleted);                        -- 逻辑删除过滤
CREATE INDEX idx_doc_format   ON knowledge_doc(doc_format);                     -- 按格式筛选（运营统计用）
-- 上传查重的关键索引：(kb_id, content_hash) 二元组唯一确定一份内容。
-- 带 WHERE deleted = 0 的部分索引，逻辑删除的旧文档不参与查重，避免"删了又传"被误判为重复。
CREATE INDEX idx_doc_kb_hash  ON knowledge_doc(kb_id, content_hash) WHERE deleted = 0;
