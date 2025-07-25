-- 知识库系统数据库表结构
-- 运行前请确保已安装 pgvector 扩展

-- 启用 pgvector 扩展
CREATE EXTENSION IF NOT EXISTS vector;

-- 知识库表 (如果不存在则创建)
CREATE TABLE IF NOT EXISTS knowledge_bases (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    subject VARCHAR(100),
    grade_level VARCHAR(50),
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建知识库索引
CREATE INDEX IF NOT EXISTS idx_knowledge_bases_name ON knowledge_bases(name);
CREATE INDEX IF NOT EXISTS idx_knowledge_bases_subject ON knowledge_bases(subject);
CREATE INDEX IF NOT EXISTS idx_knowledge_bases_created_by ON knowledge_bases(created_by);

-- 知识文档表 (如果不存在则创建)
CREATE TABLE IF NOT EXISTS knowledge_documents (
    id BIGSERIAL PRIMARY KEY,
    knowledge_base_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    file_name VARCHAR(255),
    file_type VARCHAR(50),
    file_size BIGINT,
    file_path VARCHAR(500),
    content TEXT,
    status VARCHAR(50) DEFAULT 'PENDING',
    processing_error TEXT,
    processing_progress DECIMAL(5,2) DEFAULT 0.0,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- 外键约束 (假设 knowledge_bases 表已存在)
    CONSTRAINT fk_knowledge_documents_knowledge_base 
        FOREIGN KEY (knowledge_base_id) REFERENCES knowledge_bases(id) ON DELETE CASCADE
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_knowledge_documents_knowledge_base_id ON knowledge_documents(knowledge_base_id);
CREATE INDEX IF NOT EXISTS idx_knowledge_documents_status ON knowledge_documents(status);
CREATE INDEX IF NOT EXISTS idx_knowledge_documents_created_at ON knowledge_documents(created_at);

-- 知识点表 (如果不存在则创建)
CREATE TABLE IF NOT EXISTS knowledge_points (
    id BIGSERIAL PRIMARY KEY,
    knowledge_base_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    point_type VARCHAR(50) DEFAULT 'CONCEPT',
    difficulty_level VARCHAR(20) DEFAULT 'MEDIUM',
    subject VARCHAR(100),
    tags TEXT[], -- PostgreSQL 数组类型
    source_document_id BIGINT,
    ai_confidence DECIMAL(3,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- 外键约束
    CONSTRAINT fk_knowledge_points_knowledge_base 
        FOREIGN KEY (knowledge_base_id) REFERENCES knowledge_bases(id) ON DELETE CASCADE,
    CONSTRAINT fk_knowledge_points_source_document 
        FOREIGN KEY (source_document_id) REFERENCES knowledge_documents(id) ON DELETE SET NULL
);

-- 创建知识点索引
CREATE INDEX IF NOT EXISTS idx_knowledge_points_knowledge_base_id ON knowledge_points(knowledge_base_id);
CREATE INDEX IF NOT EXISTS idx_knowledge_points_subject ON knowledge_points(subject);
CREATE INDEX IF NOT EXISTS idx_knowledge_points_difficulty ON knowledge_points(difficulty_level);
CREATE INDEX IF NOT EXISTS idx_knowledge_points_source_document ON knowledge_points(source_document_id);

-- Spring AI 向量存储表
CREATE TABLE IF NOT EXISTS vector_store (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    content TEXT NOT NULL,
    metadata JSONB,
    embedding vector(1536), -- OpenAI embedding 维度
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 向量相似度搜索索引
CREATE INDEX IF NOT EXISTS vector_store_embedding_idx ON vector_store USING hnsw (embedding vector_cosine_ops);

-- 插入测试数据 (如果表为空)
INSERT INTO knowledge_documents (knowledge_base_id, title, file_name, file_type, file_size, content, status, description)
SELECT 1, '测试文档', 'test.txt', '.txt', 1024, '这是一个测试文档内容，包含一些基础的数学概念。', 'COMPLETED', '用于测试的示例文档'
WHERE NOT EXISTS (SELECT 1 FROM knowledge_documents WHERE title = '测试文档');

-- 更新时间戳触发器
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- 为知识文档表创建更新时间戳触发器
DROP TRIGGER IF EXISTS update_knowledge_documents_updated_at ON knowledge_documents;
CREATE TRIGGER update_knowledge_documents_updated_at
    BEFORE UPDATE ON knowledge_documents
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- 为知识点表创建更新时间戳触发器
DROP TRIGGER IF EXISTS update_knowledge_points_updated_at ON knowledge_points;
CREATE TRIGGER update_knowledge_points_updated_at
    BEFORE UPDATE ON knowledge_points
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- 显示创建的表
\dt;
