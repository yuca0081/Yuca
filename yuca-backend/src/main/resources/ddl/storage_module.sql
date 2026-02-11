-- ============================================
-- 存储模块 DDL
-- ============================================

-- 文件存储表
CREATE TABLE IF NOT EXISTS storage_file (
    id              BIGSERIAL PRIMARY KEY,
    uploaded_by     BIGINT,
    file_name       VARCHAR(255) NOT NULL,
    file_size       BIGINT NOT NULL,
    content_type    VARCHAR(100),
    object_name     VARCHAR(500) NOT NULL,
    file_url        VARCHAR(1000),
    file_type       VARCHAR(50),
    source_type     VARCHAR(50),
    business_type   VARCHAR(50),
    business_id     BIGINT,
    metadata        TEXT,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted         SMALLINT DEFAULT 0
);

COMMENT ON TABLE storage_file IS '文件存储记录表';
COMMENT ON COLUMN storage_file.id IS '主键ID';
COMMENT ON COLUMN storage_file.uploaded_by IS '上传者ID（NULL表示系统文件）';
COMMENT ON COLUMN storage_file.file_name IS '原始文件名';
COMMENT ON COLUMN storage_file.file_size IS '文件大小（字节）';
COMMENT ON COLUMN storage_file.content_type IS 'MIME类型';
COMMENT ON COLUMN storage_file.object_name IS 'MinIO对象名称';
COMMENT ON COLUMN storage_file.file_url IS '文件访问URL';
COMMENT ON COLUMN storage_file.file_type IS '文件分类';
COMMENT ON COLUMN storage_file.source_type IS '来源类型: user-用户上传, system-系统生成, task-任务生成, sync-同步, api-API导入, export-导出文件';
COMMENT ON COLUMN storage_file.business_type IS '业务类型（如: avatar, document, knowledge, export等）';
COMMENT ON COLUMN storage_file.business_id IS '业务关联ID';
COMMENT ON COLUMN storage_file.metadata IS '扩展元数据（JSON格式）';
COMMENT ON COLUMN storage_file.created_at IS '创建时间';
COMMENT ON COLUMN storage_file.updated_at IS '更新时间';
COMMENT ON COLUMN storage_file.deleted IS '逻辑删除: 1-已删除, 0-未删除';

-- 索引
CREATE INDEX IF NOT EXISTS idx_storage_file_uploaded_by ON storage_file(uploaded_by) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_storage_file_object_name ON storage_file(object_name) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_storage_file_file_type ON storage_file(file_type) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_storage_file_source_type ON storage_file(source_type) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_storage_file_business ON storage_file(business_type, business_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_storage_file_created_at ON storage_file(created_at) WHERE deleted = 0;
