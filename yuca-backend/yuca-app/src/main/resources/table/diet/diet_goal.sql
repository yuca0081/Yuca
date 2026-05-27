-- ============================================
-- 用户饮食目标表
-- ============================================
CREATE TABLE IF NOT EXISTS diet_goal (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL UNIQUE,
    daily_calories  INT DEFAULT 2000,
    protein_ratio   DECIMAL(4,2) DEFAULT 20.00,
    fat_ratio       DECIMAL(4,2) DEFAULT 30.00,
    carbs_ratio     DECIMAL(4,2) DEFAULT 50.00,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_diet_goal_ratios CHECK (protein_ratio + fat_ratio + carbs_ratio = 100)
);

COMMENT ON TABLE diet_goal IS '用户饮食目标表';
COMMENT ON COLUMN diet_goal.user_id IS '用户ID';
COMMENT ON COLUMN diet_goal.daily_calories IS '每日热量目标（kcal）';
COMMENT ON COLUMN diet_goal.protein_ratio IS '蛋白质目标占比（%）';
COMMENT ON COLUMN diet_goal.fat_ratio IS '脂肪目标占比（%）';
COMMENT ON COLUMN diet_goal.carbs_ratio IS '碳水目标占比（%）';

CREATE INDEX idx_diet_goal_user ON diet_goal(user_id);
