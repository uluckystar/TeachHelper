-- 学科年级关联表
CREATE TABLE IF NOT EXISTS subject_grade_mappings (
    id BIGSERIAL PRIMARY KEY,
    subject_id BIGINT NOT NULL,
    grade_level_id BIGINT NOT NULL,
    priority INTEGER DEFAULT 0, -- 优先级，数字越小优先级越高
    is_recommended BOOLEAN DEFAULT TRUE, -- 是否推荐
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- 外键约束
    CONSTRAINT fk_subject_grade_subject 
        FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE,
    CONSTRAINT fk_subject_grade_grade_level 
        FOREIGN KEY (grade_level_id) REFERENCES grade_levels(id) ON DELETE CASCADE,
        
    -- 唯一约束（一个学科在一个年级只能有一个关联记录）
    CONSTRAINT uk_subject_grade_mapping 
        UNIQUE (subject_id, grade_level_id)
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_subject_grade_subject_id ON subject_grade_mappings(subject_id);
CREATE INDEX IF NOT EXISTS idx_subject_grade_grade_level_id ON subject_grade_mappings(grade_level_id);
CREATE INDEX IF NOT EXISTS idx_subject_grade_priority ON subject_grade_mappings(priority);
CREATE INDEX IF NOT EXISTS idx_subject_grade_recommended ON subject_grade_mappings(is_recommended);

-- 添加注释
COMMENT ON TABLE subject_grade_mappings IS '学科年级关联表';
COMMENT ON COLUMN subject_grade_mappings.subject_id IS '学科ID';
COMMENT ON COLUMN subject_grade_mappings.grade_level_id IS '年级ID';
COMMENT ON COLUMN subject_grade_mappings.priority IS '优先级，数字越小优先级越高';
COMMENT ON COLUMN subject_grade_mappings.is_recommended IS '是否推荐';
