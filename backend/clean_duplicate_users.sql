-- 清理users表中的重复数据
-- 基于数据分析：id 51-54 是重复的无效记录，可以安全删除
-- 执行前请先备份数据！

-- 1. 检查要删除的重复记录
SELECT 'Step 1: 检查即将删除的重复记录' as info;

SELECT 
    u.id,
    u.username,
    u.real_name,
    u.student_number,
    ur.role,
    (SELECT COUNT(*) FROM student_answers sa WHERE sa.student_id = u.id) as answer_count,
    'WILL_DELETE' as status
FROM users u
LEFT JOIN user_roles ur ON u.id = ur.user_id
WHERE u.id IN (51, 52, 53, 54)
ORDER BY u.id;

-- 2. 确认保留的有效记录
SELECT 'Step 2: 确认保留的有效记录' as info;

SELECT 
    u.id,
    u.username,
    u.real_name,
    u.student_number,
    ur.role,
    (SELECT COUNT(*) FROM student_answers sa WHERE sa.student_id = u.id) as answer_count,
    'WILL_KEEP' as status
FROM users u
LEFT JOIN user_roles ur ON u.id = ur.user_id
WHERE u.real_name IN ('张三', '李四', '王五', '赵六') AND u.id NOT IN (51, 52, 53, 54)
ORDER BY u.id;

-- 3. 创建备份表
SELECT 'Step 3: 创建备份表' as info;

DROP TABLE IF EXISTS users_duplicate_backup;
CREATE TABLE users_duplicate_backup AS 
SELECT * FROM users WHERE id IN (51, 52, 53, 54);

-- 4. 删除重复记录
SELECT 'Step 4: 删除重复的用户记录' as info;

-- 4.1 删除user_roles中的关联记录（如果有的话）
DELETE FROM user_roles WHERE user_id IN (51, 52, 53, 54);

-- 4.2 删除users表中的重复记录
DELETE FROM users WHERE id IN (51, 52, 53, 54);

-- 5. 验证清理结果
SELECT 'Step 5: 验证清理结果' as info;

-- 5.1 检查是否还有重复的real_name
SELECT real_name, COUNT(*) as count 
FROM users 
WHERE real_name IS NOT NULL AND real_name != '' 
GROUP BY real_name 
HAVING COUNT(*) > 1;

-- 5.2 检查最终的用户列表
SELECT 
    u.id,
    u.username,
    u.real_name,
    u.student_number,
    ur.role,
    (SELECT COUNT(*) FROM student_answers sa WHERE sa.student_id = u.id) as answer_count
FROM users u
LEFT JOIN user_roles ur ON u.id = ur.user_id
WHERE u.real_name IN ('张三', '李四', '王五', '赵六')
ORDER BY u.real_name, u.id;

-- 6. 检查总体数据统计
SELECT 'Step 6: 最终数据统计' as info;

SELECT 
    'users总数' as metric,
    COUNT(*) as count
FROM users
UNION ALL
SELECT 
    'student_answers总数',
    COUNT(*)
FROM student_answers
UNION ALL
SELECT 
    'student_answers外键完整性',
    CASE 
        WHEN COUNT(sa.id) = (SELECT COUNT(*) FROM student_answers) THEN 'ALL_OK'
        ELSE 'HAS_ISSUES'
    END
FROM student_answers sa
INNER JOIN users u ON sa.student_id = u.id;
