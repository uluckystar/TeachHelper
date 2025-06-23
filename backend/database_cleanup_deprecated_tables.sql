-- 数据库重构后的完整清理脚本
-- 目标：清理students表及相关依赖，完成向users表的完全迁移
-- 执行前请确保已备份数据！

-- ================================
-- 第一步：修复exam_submissions表
-- ================================

SELECT 'Step 1: 开始修复exam_submissions表的student_id映射' as info;

-- 1.1 创建exam_submissions备份
DROP TABLE IF EXISTS exam_submissions_backup;
CREATE TABLE exam_submissions_backup AS SELECT * FROM exam_submissions;

-- 1.2 临时禁用外键约束
SET FOREIGN_KEY_CHECKS = 0;

-- 1.3 更新exam_submissions.student_id从students.id映射到users.id
UPDATE exam_submissions es
INNER JOIN students s ON es.student_id = s.id
INNER JOIN users u ON (
    (s.student_id REGEXP '^[0-9]+$' AND CAST(s.student_id AS UNSIGNED) = u.id) OR
    (s.student_id NOT REGEXP '^[0-9]+$' AND s.student_id = u.student_number)
)
SET es.student_id = u.id
WHERE u.id IS NOT NULL;

-- 1.4 验证exam_submissions更新结果
SELECT 'Step 1.4: 验证exam_submissions更新结果' as info;

SELECT 
    es.student_id,
    u.username,
    u.student_number,
    COUNT(es.id) as submission_count
FROM exam_submissions es
LEFT JOIN users u ON es.student_id = u.id
GROUP BY es.student_id, u.username, u.student_number
ORDER BY es.student_id;

-- ================================
-- 第二步：删除外键约束
-- ================================

SELECT 'Step 2: 删除指向students表的外键约束' as info;

-- 2.1 删除student_answers表的外键约束
ALTER TABLE student_answers DROP FOREIGN KEY FK7ine3irxhlo98sfub0vu686r7;

-- 2.2 删除exam_submissions表的外键约束
ALTER TABLE exam_submissions DROP FOREIGN KEY FKg9k1eim8xh3x3ws1wxqa4pnwk;

-- ================================
-- 第三步：添加新的外键约束到users表
-- ================================

SELECT 'Step 3: 添加新的外键约束到users表' as info;

-- 3.1 为student_answers添加指向users表的外键
ALTER TABLE student_answers 
ADD CONSTRAINT FK_student_answers_user_id 
FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE;

-- 3.2 为exam_submissions添加指向users表的外键
ALTER TABLE exam_submissions 
ADD CONSTRAINT FK_exam_submissions_user_id 
FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE;

-- ================================
-- 第四步：清理classroom_students表
-- ================================

SELECT 'Step 4: 处理classroom_students表' as info;

-- 4.1 检查classroom_students表结构和数据
DESCRIBE classroom_students;

-- 4.2 检查classroom_students是否引用students表
SELECT COUNT(*) as record_count FROM classroom_students;

-- 如果classroom_students表引用了students，也需要更新
-- （根据检查结果决定是否需要额外处理）

-- ================================
-- 第五步：备份并删除students表
-- ================================

SELECT 'Step 5: 备份并删除students表' as info;

-- 5.1 创建students表的最终备份
DROP TABLE IF EXISTS students_final_backup;
CREATE TABLE students_final_backup AS SELECT * FROM students;

-- 5.2 删除students表
DROP TABLE students;

-- ================================
-- 第六步：清理其他备份表
-- ================================

SELECT 'Step 6: 清理临时备份表' as info;

-- 清理之前创建的临时备份表（可选，根据需要保留）
-- DROP TABLE IF EXISTS student_answers_backup;
-- DROP TABLE IF EXISTS users_duplicate_backup;

-- ================================
-- 第七步：验证最终结果
-- ================================

SELECT 'Step 7: 验证数据库清理结果' as info;

-- 7.1 检查剩余的表
SELECT 'Step 7.1: 当前数据库表列表' as info;
SHOW TABLES;

-- 7.2 验证student_answers外键完整性
SELECT 'Step 7.2: 验证student_answers外键完整性' as info;

SELECT 
    CASE 
        WHEN COUNT(sa.id) = (SELECT COUNT(*) FROM student_answers) THEN 'ALL_FOREIGN_KEYS_VALID'
        ELSE 'FOREIGN_KEY_VIOLATIONS_EXIST'
    END as student_answers_fk_status
FROM student_answers sa
INNER JOIN users u ON sa.student_id = u.id;

-- 7.3 验证exam_submissions外键完整性
SELECT 'Step 7.3: 验证exam_submissions外键完整性' as info;

SELECT 
    CASE 
        WHEN COUNT(es.id) = (SELECT COUNT(*) FROM exam_submissions) THEN 'ALL_FOREIGN_KEYS_VALID'
        ELSE 'FOREIGN_KEY_VIOLATIONS_EXIST'
    END as exam_submissions_fk_status
FROM exam_submissions es
INNER JOIN users u ON es.student_id = u.id;

-- 7.4 最终数据统计
SELECT 'Step 7.4: 最终数据统计' as info;

SELECT 
    'users' as table_name,
    COUNT(*) as record_count
FROM users
UNION ALL
SELECT 
    'student_answers',
    COUNT(*)
FROM student_answers
UNION ALL
SELECT 
    'exam_submissions',
    COUNT(*)
FROM exam_submissions
UNION ALL
SELECT 
    'user_roles',
    COUNT(*)
FROM user_roles;

-- ================================
-- 第八步：重新启用外键约束检查
-- ================================

SET FOREIGN_KEY_CHECKS = 1;
SELECT 'Step 8: 数据库清理完成，已重新启用外键约束检查' as info;
    REFERENCED_TABLE_NAME,
    REFERENCED_COLUMN_NAME
FROM information_schema.KEY_COLUMN_USAGE 
WHERE REFERENCED_TABLE_NAME = 'students'
  AND TABLE_SCHEMA = 'teach_helper';

-- 1.2 检查students表数据量
SELECT 'students表记录数:' as info, COUNT(*) as count FROM students;

-- 1.3 检查备份表数据量
SELECT 'student_answers_backup记录数:' as info, COUNT(*) as count FROM student_answers_backup;
SELECT 'users_duplicate_backup记录数:' as info, COUNT(*) as count FROM users_duplicate_backup;

-- 2. 检查classroom_students表是否需要更新
SELECT 'Step 2: 检查classroom_students表的student_id字段' as info;

DESCRIBE classroom_students;

-- 检查classroom_students.student_id是否指向正确的users表
SELECT 
    cs.student_id,
    u.username,
    u.student_number,
    COUNT(*) as classroom_count
FROM classroom_students cs
LEFT JOIN users u ON cs.student_id = u.id
GROUP BY cs.student_id, u.username, u.student_number
ORDER BY cs.student_id;

-- 3. 检查exam_submissions表的student_id字段
SELECT 'Step 3: 检查exam_submissions表的student_id字段' as info;

DESCRIBE exam_submissions;

-- 检查exam_submissions.student_id是否指向正确的users表
SELECT 
    es.student_id,
    u.username,
    u.student_number,
    COUNT(*) as submission_count
FROM exam_submissions es
LEFT JOIN users u ON es.student_id = u.id
GROUP BY es.student_id, u.username, u.student_number
ORDER BY es.student_id;

-- 4. 安全删除策略检查
SELECT 'Step 4: 准备安全删除策略' as info;

-- 4.1 检查是否所有外键都已正确指向users表
SELECT 'student_answers外键检查:' as table_name,
    CASE 
        WHEN COUNT(*) = 0 THEN 'ALL_REFERENCES_VALID'
        ELSE CONCAT('INVALID_REFERENCES: ', COUNT(*))
    END as status
FROM student_answers sa
LEFT JOIN users u ON sa.student_id = u.id
WHERE u.id IS NULL;

-- 5. 创建最终清理脚本模板
SELECT 'Step 5: 清理脚本准备完成' as info;
SELECT '请手动执行以下步骤：' as instruction;
SELECT '1. 确认所有外键都指向users表' as step1;
SELECT '2. 备份students表数据' as step2;
SELECT '3. 删除students表外键约束' as step3;
SELECT '4. 删除students表' as step4;
SELECT '5. 删除临时备份表' as step5;
