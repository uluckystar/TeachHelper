-- 为试卷模板题目表添加确认状态字段
-- 用于支持用户手动确认题目格式和内容

-- 添加 is_confirmed 字段
ALTER TABLE exam_template_questions 
ADD COLUMN is_confirmed TINYINT(1) DEFAULT FALSE COMMENT '是否已确认题目格式和内容';

-- 创建索引以提高查询性能
CREATE INDEX idx_exam_template_questions_confirmed 
ON exam_template_questions(exam_template_id, is_confirmed);

-- 更新说明注释
ALTER TABLE exam_template_questions 
COMMENT = '试卷模板题目表 - 支持匹配和手动确认模式'; 