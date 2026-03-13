-- ============================================
-- 用户模块 DDL
-- ============================================

-- 用户表
CREATE TABLE IF NOT EXISTS user_user (
    id              BIGSERIAL PRIMARY KEY,
    username        VARCHAR(50) NOT NULL UNIQUE,
    email           VARCHAR(100) UNIQUE,
    phone           VARCHAR(20) UNIQUE,
    password        VARCHAR(255) NOT NULL,
    nickname        VARCHAR(50),
    avatar_url      VARCHAR(255),
    status          SMALLINT DEFAULT 1,
    deleted         SMALLINT DEFAULT 0,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login_time TIMESTAMP,
    last_login_ip   VARCHAR(50)
);

COMMENT ON TABLE user_user IS '用户表';
COMMENT ON COLUMN user_user.id IS '用户ID';
COMMENT ON COLUMN user_user.username IS '用户名';
COMMENT ON COLUMN user_user.email IS '邮箱';
COMMENT ON COLUMN user_user.phone IS '手机号';
COMMENT ON COLUMN user_user.password IS '密码(BCrypt加密)';
COMMENT ON COLUMN user_user.nickname IS '昵称';
COMMENT ON COLUMN user_user.avatar_url IS '头像URL';
COMMENT ON COLUMN user_user.status IS '状态: 1-正常, 0-禁用';
COMMENT ON COLUMN user_user.deleted IS '逻辑删除: 1-已删除, 0-未删除';
COMMENT ON COLUMN user_user.create_time IS '创建时间';
COMMENT ON COLUMN user_user.update_time IS '更新时间';
COMMENT ON COLUMN user_user.last_login_time IS '最后登录时间';
COMMENT ON COLUMN user_user.last_login_ip IS '最后登录IP';

-- 刷新令牌表
CREATE TABLE IF NOT EXISTS user_refresh_token (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    token           VARCHAR(1000) NOT NULL UNIQUE,
    expiry_time     TIMESTAMP NOT NULL,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    revoked         SMALLINT DEFAULT 0
);

COMMENT ON TABLE user_refresh_token IS '刷新令牌表';
COMMENT ON COLUMN user_refresh_token.id IS '主键ID';
COMMENT ON COLUMN user_refresh_token.user_id IS '用户ID';
COMMENT ON COLUMN user_refresh_token.token IS '刷新令牌';
COMMENT ON COLUMN user_refresh_token.expiry_time IS '过期时间';
COMMENT ON COLUMN user_refresh_token.created_at IS '创建时间';
COMMENT ON COLUMN user_refresh_token.revoked IS '是否已撤销: 1-已撤销, 0-未撤销';

-- 验证码表（预留）
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

-- 索引
CREATE INDEX IF NOT EXISTS idx_user_username ON user_user(username) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_user_email ON user_user(email) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_user_phone ON user_user(phone) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_user_status ON user_user(status);

CREATE INDEX IF NOT EXISTS idx_refresh_token_user ON user_refresh_token(user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_token_expiry ON user_refresh_token(expiry_time);
CREATE INDEX IF NOT EXISTS idx_refresh_token_revoked ON user_refresh_token(revoked);

CREATE INDEX IF NOT EXISTS idx_verification_code_phone ON user_verification_code(phone);
CREATE INDEX IF NOT EXISTS idx_verification_code_expiry ON user_verification_code(expiry_time);
CREATE INDEX IF NOT EXISTS idx_verification_code_used ON user_verification_code(used);
