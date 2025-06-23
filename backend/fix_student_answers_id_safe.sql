-- 修复student_answers表的student_id字段（禁用外键约束版本）
-- 执行前请先备份数据！

-- 0. 禁用外键约束检查
SET FOREIGN_KEY_CHECKS = 0;

-- 1. 检查当前数据状况
SELECT 'Step 1: 检查当前student_answers与students、users的映射关系' as info;

SELECT 
    sa.student_id as current_student_id,
    s.student_id as students_student_id, 
    u.id as target_users_id,
    u.username,
    COUNT(sa.id) as answer_count
FROM student_answers sa
LEFT JOIN students s ON sa.student_id = s.id  
LEFT JOIN users u ON s.student_id = u.id
GROUP BY sa.student_id, s.student_id, u.id, u.username
ORDER BY sa.student_id;

-- 2. 创建临时表备份student_answers
SELECT 'Step 2: 创建备份表' as info;

DROP TABLE IF EXISTS student_answers_backup;
CREATE TABLE student_answers_backup AS SELECT * FROM student_answers;

-- 3. 处理无法映射到users表的记录（删除或标记）
SELECT 'Step 3: 处理无法映射的记录' as info;

-- 3.1 查看无法映射的记录详情
SELECT 
    sa.id as answer_id,
    sa.student_id as old_student_id,
    s.student_id as students_student_id,
    s.name as student_name,
    sa.question_id,
    sa.answer_text
FROM student_answers sa
LEFT JOIN students s ON sa.student_id = s.id
LEFT JOIN users u ON s.student_id = u.id  
WHERE u.id IS NULL;

-- 3.2 为无法映射的students记录在users表中创建对应记录
INSERT INTO users (username, password, role, student_number, real_name, created_at)
SELECT 
    s.student_id as username,
    '$2a$10$defaultpassword' as password,  -- 临时密码，需要重置
    'STUDENT' as role,
    s.student_number,
    s.name as real_name,
    NOW() as created_at
FROM students s
LEFT JOIN users u ON s.student_id = u.id
WHERE u.id IS NULL
AND s.id IN (SELECT DISTINCT student_id FROM student_answers);

-- 4. 更新student_answers.student_id，将其从students.id映射到users.id
SELECT 'Step 4: 开始更新student_answers.student_id映射' as info;

UPDATE student_answers sa
INNER JOIN students s ON sa.student_id = s.id
INNER JOIN users u ON s.student_id = u.id
SET sa.student_id = u.id
WHERE u.id IS NOT NULL;

-- 5. 验证更新结果
SELECT 'Step 5: 验证更新后的数据' as info;

SELECT 
    sa.student_id as new_student_id,
    u.username,
    u.student_number,
    u.role,
    COUNT(sa.id) as answer_count
FROM student_answers sa
LEFT JOIN users u ON sa.student_id = u.id
GROUP BY sa.student_id, u.username, u.student_number, u.role
ORDER BY sa.student_id;

-- 6. 检查外键约束兼容性
SELECT 'Step 6: 检查外键约束兼容性' as info;

SELECT 
    sa.student_id,
    CASE 
        WHEN u.id IS NOT NULL THEN 'OK'
        ELSE 'CONSTRAINT_VIOLATION'
    END as fk_status,
    COUNT(*) as count
FROM student_answers sa
LEFT JOIN users u ON sa.student_id = u.id
GROUP BY sa.student_id, fk_status
ORDER BY fk_status, sa.student_id;

-- 7. 重新启用外键约束检查
SET FOREIGN_KEY_CHECKS = 1;
