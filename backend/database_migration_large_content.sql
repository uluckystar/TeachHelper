-- 数据库迁移脚本：修复文档内容字段长度限制
-- 解决 "Data too long for column 'content'" 问题

USE teach_helper;

-- 修改 knowledge_documents 表的字段类型
-- 将 TEXT 改为 LONGTEXT 以支持大文档

-- 1. 修改 content 字段 (核心问题)
ALTER TABLE knowledge_documents MODIFY COLUMN content LONGTEXT;

-- 2. 修改 processing_error 字段 (可能包含长错误信息)
ALTER TABLE knowledge_documents MODIFY COLUMN processing_error LONGTEXT;

-- 3. 修改 description 字段 (可能包含长描述)
ALTER TABLE knowledge_documents MODIFY COLUMN description LONGTEXT;

-- 验证修改结果
DESCRIBE knowledge_documents;

-- 显示修改后的表结构
SHOW CREATE TABLE knowledge_documents;

-- 检查现有数据是否完整
SELECT 
    id, 
    title, 
    file_name,
    file_type,
    file_size,
    CHAR_LENGTH(content) as content_length,
    status,
    created_at
FROM knowledge_documents 
WHERE content IS NOT NULL
ORDER BY created_at DESC
LIMIT 10;
