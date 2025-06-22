-- 创建考试提交记录表
CREATE TABLE IF NOT EXISTS exam_submissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    exam_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    submitted_at DATETIME NOT NULL,
    auto_submitted BOOLEAN NOT NULL DEFAULT FALSE,
    submission_note TEXT,
    total_questions INT,
    answered_questions INT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- 外键约束
    FOREIGN KEY (exam_id) REFERENCES exams(id) ON DELETE CASCADE,
    FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    
    -- 唯一约束：每个学生只能提交一次同一个考试
    UNIQUE KEY uk_exam_submission (exam_id, student_id),
    
    -- 索引
    INDEX idx_exam_submissions_exam_id (exam_id),
    INDEX idx_exam_submissions_student_id (student_id),
    INDEX idx_exam_submissions_submitted_at (submitted_at)
);

-- 添加注释
ALTER TABLE exam_submissions COMMENT = '考试提交记录表';
ALTER TABLE exam_submissions MODIFY COLUMN id BIGINT AUTO_INCREMENT COMMENT '主键ID';
ALTER TABLE exam_submissions MODIFY COLUMN exam_id BIGINT NOT NULL COMMENT '考试ID';
ALTER TABLE exam_submissions MODIFY COLUMN student_id BIGINT NOT NULL COMMENT '学生ID';
ALTER TABLE exam_submissions MODIFY COLUMN submitted_at DATETIME NOT NULL COMMENT '提交时间';
ALTER TABLE exam_submissions MODIFY COLUMN auto_submitted BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否自动提交';
ALTER TABLE exam_submissions MODIFY COLUMN submission_note TEXT COMMENT '提交备注';
ALTER TABLE exam_submissions MODIFY COLUMN total_questions INT COMMENT '总题目数';
ALTER TABLE exam_submissions MODIFY COLUMN answered_questions INT COMMENT '已答题目数';
