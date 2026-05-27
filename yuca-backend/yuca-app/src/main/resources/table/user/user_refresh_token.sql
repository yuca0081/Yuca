-- ============================================
-- 刷新令牌表
-- ============================================
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

CREATE INDEX IF NOT EXISTS idx_refresh_token_user ON user_refresh_token(user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_token_expiry ON user_refresh_token(expiry_time);
CREATE INDEX IF NOT EXISTS idx_refresh_token_revoked ON user_refresh_token(revoked);
