-- V5: 修改questions表的exam_id字段为可空
-- 用于支持题库题目（不关联特定考试的题目）

-- 修改exam_id字段为可空
ALTER TABLE questions MODIFY COLUMN exam_id BIGINT NULL COMMENT '考试ID，可以为空表示题库题目';

-- 创建索引以提高查询性能
CREATE INDEX IF NOT EXISTS idx_questions_exam_id ON questions(exam_id);
CREATE INDEX IF NOT EXISTS idx_questions_created_by ON questions(created_by);
CREATE INDEX IF NOT EXISTS idx_questions_source_type ON questions(source_type);

-- 添加注释
ALTER TABLE questions COMMENT = '题目表，支持考试题目和题库题目两种类型'; 