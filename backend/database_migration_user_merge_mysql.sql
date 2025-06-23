-- 用户数据结构优化：将students表数据合并到users表（MySQL版本）
-- 注意：执行前请备份数据库！

-- 第一步：为users表添加扩展字段（MySQL语法）
-- 检查字段是否存在，不存在则添加
SET @sql = '';

-- 添加学生扩展字段
SELECT COUNT(*) INTO @exists 
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = 'teach_helper' 
  AND TABLE_NAME = 'users' 
  AND COLUMN_NAME = 'student_number';

SET @sql = IF(@exists = 0, 
  'ALTER TABLE users ADD COLUMN student_number VARCHAR(20) UNIQUE',
  'SELECT "student_number column already exists"');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT COUNT(*) INTO @exists 
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = 'teach_helper' 
  AND TABLE_NAME = 'users' 
  AND COLUMN_NAME = 'class_name';

SET @sql = IF(@exists = 0, 
  'ALTER TABLE users ADD COLUMN class_name VARCHAR(100)',
  'SELECT "class_name column already exists"');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT COUNT(*) INTO @exists 
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = 'teach_helper' 
  AND TABLE_NAME = 'users' 
  AND COLUMN_NAME = 'major';

SET @sql = IF(@exists = 0, 
  'ALTER TABLE users ADD COLUMN major VARCHAR(100)',
  'SELECT "major column already exists"');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加教师扩展字段
SELECT COUNT(*) INTO @exists 
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = 'teach_helper' 
  AND TABLE_NAME = 'users' 
  AND COLUMN_NAME = 'employee_number';

SET @sql = IF(@exists = 0, 
  'ALTER TABLE users ADD COLUMN employee_number VARCHAR(20) UNIQUE',
  'SELECT "employee_number column already exists"');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT COUNT(*) INTO @exists 
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = 'teach_helper' 
  AND TABLE_NAME = 'users' 
  AND COLUMN_NAME = 'department';

SET @sql = IF(@exists = 0, 
  'ALTER TABLE users ADD COLUMN department VARCHAR(100)',
  'SELECT "department column already exists"');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT COUNT(*) INTO @exists 
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = 'teach_helper' 
  AND TABLE_NAME = 'users' 
  AND COLUMN_NAME = 'title';

SET @sql = IF(@exists = 0, 
  'ALTER TABLE users ADD COLUMN title VARCHAR(50)',
  'SELECT "title column already exists"');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加通用扩展字段
SELECT COUNT(*) INTO @exists 
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = 'teach_helper' 
  AND TABLE_NAME = 'users' 
  AND COLUMN_NAME = 'real_name';

SET @sql = IF(@exists = 0, 
  'ALTER TABLE users ADD COLUMN real_name VARCHAR(100)',
  'SELECT "real_name column already exists"');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT COUNT(*) INTO @exists 
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = 'teach_helper' 
  AND TABLE_NAME = 'users' 
  AND COLUMN_NAME = 'phone';

SET @sql = IF(@exists = 0, 
  'ALTER TABLE users ADD COLUMN phone VARCHAR(20)',
  'SELECT "phone column already exists"');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT COUNT(*) INTO @exists 
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = 'teach_helper' 
  AND TABLE_NAME = 'users' 
  AND COLUMN_NAME = 'avatar_url';

SET @sql = IF(@exists = 0, 
  'ALTER TABLE users ADD COLUMN avatar_url VARCHAR(500)',
  'SELECT "avatar_url column already exists"');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

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
UPDATE users u
JOIN user_roles ur ON u.id = ur.user_id
JOIN students s ON s.student_id = CAST(u.id AS CHAR)
SET 
    u.student_number = s.student_number,
    u.class_name = s.class_name,
    u.major = s.major,
    u.real_name = COALESCE(s.name, u.username)
WHERE ur.role = 'STUDENT'
  AND u.student_number IS NULL; -- 避免重复更新

-- 第五步：为没有学号的学生用户生成学号
-- 注意：这里使用存储过程来处理循环逻辑
DELIMITER $$

DROP PROCEDURE IF EXISTS GenerateStudentNumbers$$

CREATE PROCEDURE GenerateStudentNumbers()
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE user_id_val BIGINT;
    DECLARE max_student_num INT DEFAULT 0;
    DECLARE new_student_number VARCHAR(20);
    
    -- 声明游标
    DECLARE user_cursor CURSOR FOR 
        SELECT u.id 
        FROM users u
        JOIN user_roles ur ON u.id = ur.user_id
        WHERE ur.role = 'STUDENT' 
          AND u.student_number IS NULL
        ORDER BY u.id;
    
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    
    -- 获取当前最大学号的数字部分
    SELECT COALESCE(MAX(CAST(SUBSTRING(student_number, 3) AS UNSIGNED)), 0)
    INTO max_student_num
    FROM users 
    WHERE student_number IS NOT NULL 
      AND student_number REGEXP '^ST[0-9]+$';
    
    OPEN user_cursor;
    
    read_loop: LOOP
        FETCH user_cursor INTO user_id_val;
        IF done THEN
            LEAVE read_loop;
        END IF;
        
        SET max_student_num = max_student_num + 1;
        SET new_student_number = CONCAT('ST', LPAD(max_student_num, 6, '0'));
        
        UPDATE users 
        SET student_number = new_student_number 
        WHERE id = user_id_val;
    END LOOP;
    
    CLOSE user_cursor;
END$$

DELIMITER ;

-- 调用存储过程
CALL GenerateStudentNumbers();

-- 删除存储过程
DROP PROCEDURE GenerateStudentNumbers;

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
LEFT JOIN students s ON s.student_id = CAST(u.id AS CHAR)
WHERE ur.role = 'STUDENT'
ORDER BY u.id
LIMIT 10;

-- 注意：
-- 1. 暂时保留students表，待完全验证后再删除
-- 2. 如需回滚，可以反向操作恢复数据
-- 3. 建议在测试环境充分验证后再在生产环境执行
