-- 添加学号字段到 students 表
-- 执行时间：2025-06-22
-- 目的：为现有的 students 表添加真正的学号字段

USE teach_helper;

-- 1. 添加学号字段
ALTER TABLE students ADD COLUMN student_number VARCHAR(20) UNIQUE COMMENT '学号';

-- 2. 为现有学生生成学号
-- 基于 student_id (实际是 users.id) 生成学号
UPDATE students 
SET student_number = CONCAT('STU', LPAD(student_id, 6, '0')) 
WHERE student_number IS NULL;

-- 3. 查看结果
SELECT id, student_id, student_number, name, email 
FROM students 
ORDER BY id 
LIMIT 10;

-- 4. 验证唯一性
SELECT student_number, COUNT(*) 
FROM students 
GROUP BY student_number 
HAVING COUNT(*) > 1;
