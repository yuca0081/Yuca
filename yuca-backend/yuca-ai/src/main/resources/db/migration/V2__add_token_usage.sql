-- 添加 token_usage 字段
ALTER TABLE ai_chat_history ADD COLUMN IF NOT EXISTS token_usage JSONB;

COMMENT ON COLUMN ai_chat_history.token_usage IS 'Token使用统计，JSON格式，如 {"inputTokenCount":10,"outputTokenCount":20,"totalTokenCount":30}';
