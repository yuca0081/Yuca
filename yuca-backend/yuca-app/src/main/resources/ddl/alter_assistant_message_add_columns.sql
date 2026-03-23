-- =====================================================
-- 迁移脚本：为 assistant_message 表添加缺失的字段
-- 创建时间: 2025-03-23
-- 说明：添加 token 统计字段和模型名称字段
-- =====================================================

-- 添加 model_name 字段
ALTER TABLE assistant_message ADD COLUMN IF NOT EXISTS model_name VARCHAR(50);

-- 添加 thinking_content 字段
ALTER TABLE assistant_message ADD COLUMN IF NOT EXISTS thinking_content TEXT;

-- 添加 input_tokens 字段
ALTER TABLE assistant_message ADD COLUMN IF NOT EXISTS input_tokens INTEGER DEFAULT 0;

-- 添加 output_tokens 字段
ALTER TABLE assistant_message ADD COLUMN IF NOT EXISTS output_tokens INTEGER DEFAULT 0;

-- 添加 prompt_tokens_details 字段
ALTER TABLE assistant_message ADD COLUMN IF NOT EXISTS prompt_tokens_details TEXT;

-- 添加 total_tokens 字段
ALTER TABLE assistant_message ADD COLUMN IF NOT EXISTS total_tokens INTEGER DEFAULT 0;

-- 添加字段注释
COMMENT ON COLUMN assistant_message.model_name IS '使用的模型：qwen-plus/qwen-max等（仅assistant角色消息有值）';
COMMENT ON COLUMN assistant_message.thinking_content IS 'AI深度思考内容（可选）';
COMMENT ON COLUMN assistant_message.input_tokens IS '输入token数（prompt_tokens）';
COMMENT ON COLUMN assistant_message.output_tokens IS '输出token数（completion_tokens）';
COMMENT ON COLUMN assistant_message.prompt_tokens_details IS '输入token详细信息（JSON格式），包含：cached_tokens, audio_tokens, text_tokens等';
COMMENT ON COLUMN assistant_message.total_tokens IS '总token数（input + output）';

-- 从 assistant_session 表删除 model_name 列（如果还存在）
ALTER TABLE assistant_session DROP COLUMN IF EXISTS model_name;
