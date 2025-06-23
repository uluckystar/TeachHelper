-- 创建缺失的users记录来解决student_answers外键约束问题
-- 为STU001-STU004学生创建对应的users记录

-- 1. 检查当前问题状况
SELECT 'Step 1: 检查当前外键约束违反情况' as info;

SELECT 
    sa.student_id,
    COUNT(*) as answer_count,
    s.name as student_name,
    s.student_id as student_number_str,
    u.id as user_exists
FROM student_answers sa
LEFT JOIN students s ON sa.student_id = s.id
LEFT JOIN users u ON sa.student_id = u.id
WHERE u.id IS NULL
GROUP BY sa.student_id, s.name, s.student_id
ORDER BY sa.student_id;

-- 2. 临时禁用外键约束
SET FOREIGN_KEY_CHECKS = 0;
SELECT 'Step 2: 已禁用外键约束检查' as info;

-- 3. 为STU001-STU004学生创建users记录
-- 使用students表中的id作为users表的id，确保student_answers.student_id能正确关联
INSERT INTO users (
    id, username, email, password, student_number, real_name, 
    class_name, major, enabled, created_at, updated_at
)
SELECT 
    s.id,
    CONCAT('student_', s.student_id),
    s.email,
    '$2a$10$N7nO7z9JYKPwKJ9P6kZvSeOKgF8JaU8n8YqH8xKjXhTKzV9Y1XkC6', -- bcrypt encoded "password123"
    s.student_id,
    s.name,
    COALESCE(s.class_name, '未分班'),
    COALESCE(s.major, '计算机科学与技术'),
    1,
    NOW(),
    NOW()
FROM students s
WHERE s.id IN (9, 10, 11, 12)
AND NOT EXISTS (SELECT 1 FROM users u WHERE u.id = s.id);

SELECT 'Step 3: 已创建缺失的用户记录' as info;

-- 4. 为新创建的用户分配STUDENT角色
INSERT INTO user_roles (user_id, role)
SELECT u.id, 'STUDENT'
FROM users u
WHERE u.id IN (9, 10, 11, 12)
AND NOT EXISTS (SELECT 1 FROM user_roles ur WHERE ur.user_id = u.id AND ur.role = 'STUDENT');

SELECT 'Step 4: 已分配学生角色' as info;

-- 5. 重新启用外键约束
SET FOREIGN_KEY_CHECKS = 1;
SELECT 'Step 5: 已重新启用外键约束检查' as info;

-- 6. 验证修复结果
SELECT 'Step 6: 验证修复后的数据映射' as info;

SELECT 
    sa.student_id,
    u.username,
    u.student_number,
    u.real_name,
    ur.role,
    COUNT(sa.id) as answer_count
FROM student_answers sa
INNER JOIN users u ON sa.student_id = u.id
LEFT JOIN user_roles ur ON u.id = ur.user_id
GROUP BY sa.student_id, u.username, u.student_number, u.real_name, ur.role
ORDER BY sa.student_id;

-- 7. 最终外键约束检查
SELECT 'Step 7: 最终外键约束状态检查' as info;

SELECT 
    CASE 
        WHEN EXISTS (
            SELECT 1 FROM student_answers sa 
            LEFT JOIN users u ON sa.student_id = u.id 
            WHERE u.id IS NULL
        ) THEN 'CONSTRAINT_VIOLATION_EXISTS'
        ELSE 'ALL_CONSTRAINTS_OK'
    END as final_status,
    (SELECT COUNT(*) FROM student_answers) as total_answers,
    (SELECT COUNT(*) FROM student_answers sa INNER JOIN users u ON sa.student_id = u.id) as mapped_answers;

-- 8. 显示所有学生用户
SELECT 'Step 8: 所有学生用户列表' as info;

SELECT 
    u.id,
    u.username,
    u.student_number,
    u.real_name,
    u.email,
    ur.role
FROM users u
INNER JOIN user_roles ur ON u.id = ur.user_id
WHERE ur.role = 'STUDENT'
ORDER BY u.id;
