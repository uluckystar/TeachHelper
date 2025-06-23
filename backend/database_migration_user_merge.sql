-- 用户数据结构优化：将students表数据合并到users表
-- 注意：执行前请备份数据库！

-- 第一步：为users表添加扩展字段
ALTER TABLE users 
ADD COLUMN IF NOT EXISTS student_number VARCHAR(20) UNIQUE,
ADD COLUMN IF NOT EXISTS class_name VARCHAR(100),
ADD COLUMN IF NOT EXISTS major VARCHAR(100),
ADD COLUMN IF NOT EXISTS employee_number VARCHAR(20) UNIQUE,
ADD COLUMN IF NOT EXISTS department VARCHAR(100),
ADD COLUMN IF NOT EXISTS title VARCHAR(50),
ADD COLUMN IF NOT EXISTS real_name VARCHAR(100),
ADD COLUMN IF NOT EXISTS phone VARCHAR(20),
ADD COLUMN IF NOT EXISTS avatar_url VARCHAR(500);

-- 第二步：检查students表数据状态
SELECT 
    '学生表数据统计' as description,
    COUNT(*) as total_students,
    COUNT(student_number) as students_with_number,
    COUNT(class_name) as students_with_class,
    COUNT(major) as students_with_major
FROM students;

-- 第三步：检查users表中学生用户数据状态  
SELECT 
    '用户表学生数据统计' as description,
    COUNT(*) as total_student_users,
    COUNT(student_number) as users_with_student_number
FROM users u
JOIN user_roles ur ON u.id = ur.user_id
WHERE ur.role = 'STUDENT';

-- 第四步：将students表数据迁移到users表
-- 优先使用students表的name作为real_name，如果不存在则保持users表的username
UPDATE users u
SET 
    student_number = s.student_number,
    class_name = s.class_name,
    major = s.major,
    real_name = COALESCE(s.name, u.username)
FROM students s
JOIN user_roles ur ON u.id = ur.user_id
WHERE s.student_id = CAST(u.id AS VARCHAR)
  AND ur.role = 'STUDENT'
  AND u.student_number IS NULL; -- 避免重复更新

-- 第五步：为没有学号的学生用户生成学号
-- 查找最大现有学号
DO $$
DECLARE
    max_student_num INTEGER;
    user_record RECORD;
    new_student_number VARCHAR(20);
BEGIN
    -- 获取当前最大学号的数字部分
    SELECT COALESCE(MAX(CAST(SUBSTRING(student_number FROM 3) AS INTEGER)), 0)
    INTO max_student_num
    FROM users 
    WHERE student_number IS NOT NULL 
      AND student_number ~ '^ST[0-9]+$';
    
    -- 为没有学号的学生用户生成学号
    FOR user_record IN 
        SELECT u.id 
        FROM users u
        JOIN user_roles ur ON u.id = ur.user_id
        WHERE ur.role = 'STUDENT' 
          AND u.student_number IS NULL
        ORDER BY u.id
    LOOP
        max_student_num := max_student_num + 1;
        new_student_number := 'ST' || LPAD(max_student_num::text, 6, '0');
        
        UPDATE users 
        SET student_number = new_student_number 
        WHERE id = user_record.id;
    END LOOP;
END $$;

-- 第六步：验证迁移结果
SELECT 
    '迁移后数据验证' as description,
    COUNT(*) as total_student_users,
    COUNT(student_number) as users_with_student_number,
    COUNT(real_name) as users_with_real_name,
    COUNT(class_name) as users_with_class,
    COUNT(major) as users_with_major
FROM users u
JOIN user_roles ur ON u.id = ur.user_id
WHERE ur.role = 'STUDENT';

-- 第七步：检查数据一致性
SELECT 
    '数据一致性检查' as description,
    u.id as user_id,
    u.username,
    u.email as user_email,
    u.student_number,
    u.real_name,
    u.class_name,
    u.major,
    s.student_id,
    s.name as student_name,
    s.email as student_email,
    s.student_number as old_student_number,
    s.class_name as old_class_name,
    s.major as old_major
FROM users u
JOIN user_roles ur ON u.id = ur.user_id
LEFT JOIN students s ON s.student_id = CAST(u.id AS VARCHAR)
WHERE ur.role = 'STUDENT'
ORDER BY u.id
LIMIT 10;

-- 第八步：备份students表数据（可选，用于回滚）
-- CREATE TABLE students_backup AS SELECT * FROM students;

-- 注意：
-- 1. 暂时保留students表，待完全验证后再删除
-- 2. 如需回滚，可以反向操作恢复数据
-- 3. 建议在测试环境充分验证后再在生产环境执行
