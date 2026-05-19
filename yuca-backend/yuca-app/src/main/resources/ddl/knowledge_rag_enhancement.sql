-- =====================================================
-- RAG 增强：为 knowledge_chunk 添加全文搜索支持
-- =====================================================

-- 1. 启用 zhparser 中文分词扩展
CREATE EXTENSION IF NOT EXISTS zhparser;

-- 2. 创建中文全文搜索配置
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_ts_config WHERE cfgname = 'chinese') THEN
        CREATE TEXT SEARCH CONFIGURATION chinese (PARSER = zhparser);
        ALTER TEXT SEARCH CONFIGURATION chinese ADD MAPPING FOR n,v,a,i,e,l WITH simple;
    END IF;
END
$$;

-- 3. 添加 tsvector 列（使用中文分词配置）
ALTER TABLE knowledge_chunk
    ADD COLUMN IF NOT EXISTS fts_tokens tsvector
    GENERATED ALWAYS AS (to_tsvector('chinese', content)) STORED;

-- 4. 创建 GIN 索引加速全文搜索
CREATE INDEX IF NOT EXISTS idx_chunk_fts
    ON knowledge_chunk USING gin(fts_tokens)
    WHERE deleted = 0 AND is_active = TRUE;
