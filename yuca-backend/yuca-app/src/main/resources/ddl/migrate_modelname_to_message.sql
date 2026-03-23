-- =====================================================
-- 迁移脚本：将 model_name 从 session 表移到 message 表
-- 创建时间: 2025-03-23
-- =====================================================

-- 1. 在 assistant_message 表添加 model_name 列
ALTER TABLE assistant_message ADD COLUMN IF NOT EXISTS model_name VARCHAR(50);

-- 2. 从 assistant_session 表删除 model_name 列
-- 注意：首先需要处理数据迁移逻辑（在应用层处理）
ALTER TABLE assistant_session DROP COLUMN IF EXISTS model_name;

-- 3. 为 assistant_message.model_name 添加注释
COMMENT ON COLUMN assistant_message.model_name IS '使用的模型：qwen-plus/qwen-max等（记录该消息使用的模型）';

-- 添加缺失的字段
ALTER TABLE assistant_message ADD COLUMN IF NOT EXISTS model_name VARCHAR(50);
ALTER TABLE assistant_message ADD COLUMN IF NOT EXISTS thinking_content TEXT;
ALTER TABLE assistant_message ADD COLUMN IF NOT EXISTS input_tokens INTEGER DEFAULT 0;
ALTER TABLE assistant_message ADD COLUMN IF NOT EXISTS output_tokens INTEGER DEFAULT 0;
ALTER TABLE assistant_message ADD COLUMN IF NOT EXISTS prompt_tokens_details TEXT;
ALTER TABLE assistant_message ADD COLUMN IF NOT EXISTS total_tokens INTEGER DEFAULT 0;

-- 添加注释
COMMENT ON COLUMN assistant_message.model_name IS '使用的模型：qwen-plus/qwen-max等（仅assistant角色消息有值）';
COMMENT ON COLUMN assistant_message.thinking_content IS 'AI深度思考内容（可选）';
COMMENT ON COLUMN assistant_message.input_tokens IS '输入token数（prompt_tokens）';
COMMENT ON COLUMN assistant_message.output_tokens IS '输出token数（completion_tokens）';
COMMENT ON COLUMN assistant_message.prompt_tokens_details IS '输入token详细信息（JSON格式）';
COMMENT ON COLUMN assistant_message.total_tokens IS '总token数（input + output）';