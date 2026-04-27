-- 饮食记录模块数据库表设计
-- 创建时间：2026-04-27
-- 说明：饮食记录与目标管理

-- ============================================
-- 1. 饮食记录表
-- ============================================
CREATE TABLE IF NOT EXISTS diet_record (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL,                 -- 所属用户
    record_date     DATE NOT NULL,                   -- 记录日期
    meal_type       SMALLINT NOT NULL,               -- 餐次：1=早餐 2=午餐 3=晚餐 4=加餐
    food_name       VARCHAR(100) NOT NULL,           -- 食物名称
    amount          DECIMAL(8,2) NOT NULL,           -- 食用量
    unit            VARCHAR(10) DEFAULT 'g',         -- 单位（g/份/ml）
    calories        DECIMAL(8,2) NOT NULL,           -- 热量 kcal
    protein         DECIMAL(8,2),                    -- 蛋白质 g
    fat             DECIMAL(8,2),                    -- 脂肪 g
    carbs           DECIMAL(8,2),                    -- 碳水 g
    remark          VARCHAR(255),                    -- 备注
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted         INT DEFAULT 0,                   -- 逻辑删除
    CONSTRAINT chk_diet_record_meal_type CHECK (meal_type IN (1, 2, 3, 4))
);

COMMENT ON TABLE diet_record IS '饮食记录表';
COMMENT ON COLUMN diet_record.user_id IS '所属用户ID';
COMMENT ON COLUMN diet_record.record_date IS '记录日期';
COMMENT ON COLUMN diet_record.meal_type IS '餐次：1=早餐 2=午餐 3=晚餐 4=加餐';
COMMENT ON COLUMN diet_record.food_name IS '食物名称';
COMMENT ON COLUMN diet_record.amount IS '食用量';
COMMENT ON COLUMN diet_record.unit IS '单位（g/份/ml）';
COMMENT ON COLUMN diet_record.calories IS '热量（kcal）';
COMMENT ON COLUMN diet_record.protein IS '蛋白质（g）';
COMMENT ON COLUMN diet_record.fat IS '脂肪（g）';
COMMENT ON COLUMN diet_record.carbs IS '碳水（g）';
COMMENT ON COLUMN diet_record.remark IS '备注';
COMMENT ON COLUMN diet_record.deleted IS '逻辑删除标记：0-正常，1-已删除';

CREATE INDEX idx_diet_record_user_date ON diet_record(user_id, record_date);
CREATE INDEX idx_diet_record_deleted ON diet_record(deleted);


-- ============================================
-- 2. 用户饮食目标表
-- ============================================
CREATE TABLE IF NOT EXISTS diet_goal (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL UNIQUE,          -- 用户ID（唯一）
    daily_calories  INT DEFAULT 2000,                -- 每日热量目标 kcal
    protein_ratio   DECIMAL(4,2) DEFAULT 20.00,      -- 蛋白质目标占比 %
    fat_ratio       DECIMAL(4,2) DEFAULT 30.00,      -- 脂肪目标占比 %
    carbs_ratio     DECIMAL(4,2) DEFAULT 50.00,      -- 碳水目标占比 %
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
