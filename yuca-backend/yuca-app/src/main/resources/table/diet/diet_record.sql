-- ============================================
-- 饮食记录表
-- ============================================
CREATE TABLE IF NOT EXISTS diet_record (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    record_date     DATE NOT NULL,
    meal_type       SMALLINT NOT NULL,
    food_name       VARCHAR(100) NOT NULL,
    amount          DECIMAL(8,2) NOT NULL,
    unit            VARCHAR(10) DEFAULT 'g',
    calories        DECIMAL(8,2) NOT NULL,
    protein         DECIMAL(8,2),
    fat             DECIMAL(8,2),
    carbs           DECIMAL(8,2),
    remark          VARCHAR(255),
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted         INT DEFAULT 0,
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
