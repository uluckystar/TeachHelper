-- 创建试卷模板表
CREATE TABLE exam_paper_templates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL COMMENT '模板名称',
    description TEXT COMMENT '模板描述',
    subject VARCHAR(100) COMMENT '科目',
    grade_level VARCHAR(50) COMMENT '年级',
    total_score INT DEFAULT 100 COMMENT '总分',
    duration INT COMMENT '考试时长（分钟）',
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '状态: DRAFT, READY, PUBLISHED, ARCHIVED',
    template_type VARCHAR(30) NOT NULL DEFAULT 'MANUAL' COMMENT '模板类型: MANUAL, AI_GENERATED, DOCUMENT_EXTRACTED, COPIED',
    question_type_config TEXT COMMENT '题型配置（JSON格式）',
    difficulty_config TEXT COMMENT '难度配置（JSON格式）',
    knowledge_base_config TEXT COMMENT '知识库配置（JSON格式）',
    tags TEXT COMMENT '模板标签（JSON格式）',
    is_public BOOLEAN DEFAULT FALSE COMMENT '是否公开模板',
    usage_count INT DEFAULT 0 COMMENT '使用次数',
    created_by BIGINT NOT NULL COMMENT '创建者ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    last_used_at DATETIME COMMENT '最后使用时间',
    
    INDEX idx_name (name),
    INDEX idx_subject (subject),
    INDEX idx_grade_level (grade_level),
    INDEX idx_status (status),
    INDEX idx_template_type (template_type),
    INDEX idx_created_by (created_by),
    INDEX idx_is_public (is_public),
    INDEX idx_usage_count (usage_count),
    INDEX idx_created_at (created_at),
    INDEX idx_last_used_at (last_used_at),
    
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE CASCADE
) COMMENT '试卷模板表';

-- 创建试卷模板题目表
CREATE TABLE exam_paper_template_questions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_id BIGINT NOT NULL COMMENT '模板ID',
    question_order INT NOT NULL COMMENT '题目序号',
    question_type VARCHAR(30) NOT NULL COMMENT '题目类型',
    question_content TEXT COMMENT '题目内容',
    question_id BIGINT COMMENT '题目ID（如果引用现有题目）',
    score DECIMAL(5,2) COMMENT '分数',
    difficulty_level VARCHAR(20) COMMENT '难度等级',
    knowledge_tags TEXT COMMENT '知识点标签（JSON格式）',
    question_config TEXT COMMENT '题目配置（JSON格式）',
    is_required BOOLEAN DEFAULT TRUE COMMENT '是否必答',
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '状态: DRAFT, CONFIGURED, READY',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    INDEX idx_template_id (template_id),
    INDEX idx_question_order (question_order),
    INDEX idx_question_type (question_type),
    INDEX idx_question_id (question_id),
    INDEX idx_status (status),
    UNIQUE KEY uk_template_question_order (template_id, question_order),
    
    FOREIGN KEY (template_id) REFERENCES exam_paper_templates(id) ON DELETE CASCADE,
    FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE SET NULL
) COMMENT '试卷模板题目表';

-- 在exams表中添加source_paper_template_id字段
ALTER TABLE exams ADD COLUMN source_paper_template_id BIGINT COMMENT '来源试卷模板ID';
ALTER TABLE exams ADD INDEX idx_source_paper_template_id (source_paper_template_id);
ALTER TABLE exams ADD FOREIGN KEY (source_paper_template_id) REFERENCES exam_paper_templates(id) ON DELETE SET NULL; 