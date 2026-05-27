-- ============================================
-- 用户表
-- ============================================
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

CREATE INDEX IF NOT EXISTS idx_user_username ON user_user(username) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_user_email ON user_user(email) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_user_phone ON user_user(phone) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_user_status ON user_user(status);
