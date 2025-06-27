CREATE TABLE prompts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    name VARCHAR(255) NOT NULL COMMENT '提示词唯一名称，用于程序调用',
    description TEXT COMMENT '对提示词用途的详细描述',
    template TEXT NOT NULL COMMENT '提示词模板，使用{占位符}进行变量替换',
    version INT NOT NULL COMMENT '版本号，允许多个版本的提示词并存',
    is_active BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否为当前激活的版本',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_name_version (name, version) COMMENT '同一名称下版本号唯一'
) COMMENT '用于存储AI调用的提示词模板';