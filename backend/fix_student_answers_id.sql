-- 修复student_answers表的student_id字段，从指向students.id改为指向users.id
-- 基于JPA实体类分析：
-- - students.student_id 字段存储的是 users.id (字符串格式)
-- - student_answers.student_id 当前指向 students.id，需要改为指向 users.id
-- 执行前请先备份数据！

-- 1. 检查当前数据状况
SELECT 'Step 1: 检查当前student_answers与students、users的映射关系' as info;

SELECT 
    sa.student_id as current_student_id,
    s.student_id as students_student_id_str, 
    CAST(s.student_id AS UNSIGNED) as target_users_id,
    u.id as users_id,
    u.username,
    u.student_number,
    COUNT(sa.id) as answer_count
FROM student_answers sa
LEFT JOIN students s ON sa.student_id = s.id  
LEFT JOIN users u ON CAST(s.student_id AS UNSIGNED) = u.id
GROUP BY sa.student_id, s.student_id, u.id, u.username, u.student_number
ORDER BY sa.student_id;

-- 2. 创建临时表备份student_answers
SELECT 'Step 2: 创建备份表' as info;

DROP TABLE IF EXISTS student_answers_backup;
CREATE TABLE student_answers_backup AS SELECT * FROM student_answers;

-- 3. 更新student_answers.student_id，将其从students.id映射到users.id
SELECT 'Step 3: 开始更新student_answers.student_id映射' as info;

-- 3.1 对于student_id是数字格式且能够映射到users表的记录
UPDATE student_answers sa
INNER JOIN students s ON sa.student_id = s.id
INNER JOIN users u ON s.student_id REGEXP '^[0-9]+$' AND CAST(s.student_id AS UNSIGNED) = u.id
SET sa.student_id = u.id
WHERE u.id IS NOT NULL;

-- 3.2 对于student_id是字符串格式的记录，尝试通过student_number匹配
UPDATE student_answers sa
INNER JOIN students s ON sa.student_id = s.id
INNER JOIN users u ON s.student_id = u.student_number
SET sa.student_id = u.id
WHERE s.student_id NOT REGEXP '^[0-9]+$' AND u.id IS NOT NULL;

-- 4. 检查无法映射的记录
SELECT 'Step 4: 检查无法映射到users表的student_answers记录' as info;

SELECT 
    sa.id as answer_id,
    sa.student_id as old_student_id,
    s.student_id as students_student_id,
    s.name as student_name,
    'NO_USER_MAPPING' as issue
FROM student_answers sa
LEFT JOIN students s ON sa.student_id = s.id
LEFT JOIN users u ON (
    (s.student_id REGEXP '^[0-9]+$' AND CAST(s.student_id AS UNSIGNED) = u.id) OR
    (s.student_id NOT REGEXP '^[0-9]+$' AND s.student_id = u.student_number)
)
WHERE u.id IS NULL;

-- 5. 验证更新结果
SELECT 'Step 5: 验证更新后的数据' as info;

SELECT 
    sa.student_id as new_student_id,
    u.username,
    u.student_number,
    ur.role,
    COUNT(sa.id) as answer_count
FROM student_answers sa
LEFT JOIN users u ON sa.student_id = u.id
LEFT JOIN user_roles ur ON u.id = ur.user_id
GROUP BY sa.student_id, u.username, u.student_number, ur.role
ORDER BY sa.student_id;

-- 6. 检查是否还有外键约束问题
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
SELECT 'Step 7: 已重新启用外键约束检查' as info;
