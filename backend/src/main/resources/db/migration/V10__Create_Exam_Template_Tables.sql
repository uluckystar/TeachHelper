-- 创建考试模板表
CREATE TABLE exam_templates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_name VARCHAR(255) NOT NULL COMMENT '模板名称',
    subject VARCHAR(100) COMMENT '科目',
    exam_title VARCHAR(255) COMMENT '考试标题',
    description TEXT COMMENT '描述',
    total_questions INT COMMENT '总题目数',
    matched_questions INT DEFAULT 0 COMMENT '已匹配题目数',
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '状态: DRAFT, READY, APPLIED, ARCHIVED',
    source_files TEXT COMMENT '来源文件列表（JSON格式）',
    parse_metadata TEXT COMMENT '解析元数据（JSON格式）',
    created_by BIGINT COMMENT '创建者ID',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    INDEX idx_template_name (template_name),
    INDEX idx_subject (subject),
    INDEX idx_status (status),
    INDEX idx_created_by (created_by),
    INDEX idx_created_time (created_time),
    
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL
) COMMENT '考试模板表';

-- 创建考试模板题目表
CREATE TABLE exam_template_questions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_id BIGINT NOT NULL COMMENT '模板ID',
    question_number INT NOT NULL COMMENT '题目序号',
    question_content TEXT NOT NULL COMMENT '题目内容',
    section_header VARCHAR(255) COMMENT '段落标题',
    question_type VARCHAR(50) COMMENT '题目类型',
    score DECIMAL(5,2) COMMENT '分数',
    correct_answer TEXT COMMENT '正确答案',
    options TEXT COMMENT '选项（JSON格式）',
    explanation TEXT COMMENT '解释说明',
    is_required BOOLEAN DEFAULT TRUE COMMENT '是否必答',
    
    -- 匹配相关字段
    matched_question_id BIGINT COMMENT '匹配到的题目ID',
    is_matched BOOLEAN DEFAULT FALSE COMMENT '是否已匹配',
    matching_strategy VARCHAR(50) COMMENT '匹配策略',
    matching_confidence DECIMAL(3,2) COMMENT '匹配置信度',
    matching_reason TEXT COMMENT '匹配原因',
    
    -- 验证相关字段
    has_issues BOOLEAN DEFAULT FALSE COMMENT '是否有问题',
    issues TEXT COMMENT '问题列表（JSON格式）',
    suggestions TEXT COMMENT '建议列表（JSON格式）',
    original_index INT COMMENT '原始文档中的索引',
    source_document VARCHAR(255) COMMENT '来源文档名称',
    
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    INDEX idx_template_id (template_id),
    INDEX idx_question_number (question_number),
    INDEX idx_matched_question (matched_question_id),
    INDEX idx_is_matched (is_matched),
    INDEX idx_has_issues (has_issues),
    UNIQUE KEY uk_template_question (template_id, question_number),
    
    FOREIGN KEY (template_id) REFERENCES exam_templates(id) ON DELETE CASCADE,
    FOREIGN KEY (matched_question_id) REFERENCES questions(id) ON DELETE SET NULL
) COMMENT '考试模板题目表';

-- 为exams表添加source_template_id字段（如果不存在）
ALTER TABLE exams 
ADD COLUMN IF NOT EXISTS source_template_id BIGINT COMMENT '来源模板ID',
ADD INDEX IF NOT EXISTS idx_source_template (source_template_id);

-- 添加外键约束（如果不存在）
SET @sql = IF((SELECT COUNT(*) FROM information_schema.REFERENTIAL_CONSTRAINTS 
               WHERE CONSTRAINT_SCHEMA = DATABASE() 
               AND CONSTRAINT_NAME = 'exams_ibfk_source_template') = 0,
              'ALTER TABLE exams ADD FOREIGN KEY (source_template_id) REFERENCES exam_templates(id) ON DELETE SET NULL',
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 创建模板解析统计表
CREATE TABLE template_parse_statistics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_id BIGINT NOT NULL COMMENT '模板ID',
    total_documents INT DEFAULT 0 COMMENT '总文档数',
    successful_documents INT DEFAULT 0 COMMENT '成功解析文档数',
    failed_documents INT DEFAULT 0 COMMENT '失败文档数',
    total_questions_extracted INT DEFAULT 0 COMMENT '提取的总题目数',
    duplicate_questions_merged INT DEFAULT 0 COMMENT '合并的重复题目数',
    quality_issues_found INT DEFAULT 0 COMMENT '发现的质量问题数',
    parsing_errors TEXT COMMENT '解析错误列表（JSON格式）',
    extraction_time_seconds INT COMMENT '提取耗时（秒）',
    
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    
    INDEX idx_template_id (template_id),
    FOREIGN KEY (template_id) REFERENCES exam_templates(id) ON DELETE CASCADE
) COMMENT '模板解析统计表';