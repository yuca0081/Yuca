-- ============================================
-- 验证码表
-- ============================================
CREATE TABLE IF NOT EXISTS user_verification_code (
    id              BIGSERIAL PRIMARY KEY,
    phone           VARCHAR(20) NOT NULL,
    code            VARCHAR(10) NOT NULL,
    code_type       VARCHAR(20) NOT NULL,
    expiry_time     TIMESTAMP NOT NULL,
    used            SMALLINT DEFAULT 0,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE user_verification_code IS '验证码表';
COMMENT ON COLUMN user_verification_code.id IS '主键ID';
COMMENT ON COLUMN user_verification_code.phone IS '手机号';
COMMENT ON COLUMN user_verification_code.code IS '验证码';
COMMENT ON COLUMN user_verification_code.code_type IS '验证码类型: REGISTER-注册, LOGIN-登录, RESET_PASSWORD-重置密码';
COMMENT ON COLUMN user_verification_code.expiry_time IS '过期时间';
COMMENT ON COLUMN user_verification_code.used IS '是否已使用: 1-已使用, 0-未使用';
COMMENT ON COLUMN user_verification_code.create_time IS '创建时间';

CREATE INDEX IF NOT EXISTS idx_verification_code_phone ON user_verification_code(phone);
CREATE INDEX IF NOT EXISTS idx_verification_code_expiry ON user_verification_code(expiry_time);
CREATE INDEX IF NOT EXISTS idx_verification_code_used ON user_verification_code(used);
