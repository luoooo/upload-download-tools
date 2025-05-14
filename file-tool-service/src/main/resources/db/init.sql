-- 创建数据库
CREATE DATABASE IF NOT EXISTS file_tool DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE file_tool;

-- 文件任务表
CREATE TABLE IF NOT EXISTS file_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '任务ID',
    task_name VARCHAR(100) NOT NULL COMMENT '任务名称',
    original_filename VARCHAR(255) COMMENT '原始文件名',
    file_path VARCHAR(500) COMMENT '文件路径',
    file_size BIGINT COMMENT '文件大小(字节)',
    status VARCHAR(20) NOT NULL COMMENT '任务状态：PENDING/PROCESSING/COMPLETED/FAILED',
    task_type VARCHAR(20) NOT NULL COMMENT '任务类型：UPLOAD/DOWNLOAD',
    field_mapping TEXT COMMENT '字段映射(JSON格式)',
    callback_url VARCHAR(500) COMMENT '回调URL',
    callback_params TEXT COMMENT '回调参数(JSON格式)',
    processed_rows INT DEFAULT 0 COMMENT '已处理行数',
    success_rows INT DEFAULT 0 COMMENT '成功行数',
    failed_rows INT DEFAULT 0 COMMENT '失败行数',
    error_message TEXT COMMENT '错误信息',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_status (status),
    INDEX idx_task_type (task_type),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件任务表';

-- 文件处理记录表
CREATE TABLE file_process_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    task_id BIGINT NOT NULL COMMENT '任务ID',
    line_num INT NOT NULL COMMENT '行号',
    status VARCHAR(20) NOT NULL COMMENT '处理状态：SUCCESS-成功，FAILED-失败',
    error_message VARCHAR(500) COMMENT '错误信息',
    raw_data TEXT COMMENT '原始数据',
    processed_data TEXT COMMENT '处理后的数据',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_task_id (task_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件处理记录表'; 

-- 系统配置表
CREATE TABLE IF NOT EXISTS system_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '配置ID',
    config_key VARCHAR(50) NOT NULL COMMENT '配置键',
    config_value TEXT COMMENT '配置值',
    description VARCHAR(200) COMMENT '配置描述',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

-- 插入基础配置数据
INSERT INTO system_config (config_key, config_value, description) VALUES
('file.upload.max-size', '10485760', '文件上传大小限制(字节)'),
('file.storage.type', 'local', '文件存储类型：local/minio'),
('file.storage.enabled', 'true', '是否启用文件存储服务'),
('file.storage.local.path', './upload', '本地存储路径'),
('file.storage.minio.endpoint', 'http://localhost:9000', 'MinIO服务地址'),
('file.storage.minio.access-key', 'minioadmin', 'MinIO访问密钥'),
('file.storage.minio.secret-key', 'minioadmin', 'MinIO密钥'),
('file.storage.minio.bucket', 'file-tool', 'MinIO存储桶名称');

-- 创建测试数据库
CREATE DATABASE IF NOT EXISTS file_tool_test DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE file_tool_test;

-- 复制表结构到测试数据库
CREATE TABLE IF NOT EXISTS file_task LIKE file_tool.file_task;
CREATE TABLE IF NOT EXISTS file_process_record LIKE file_tool.file_process_record;
CREATE TABLE IF NOT EXISTS system_config LIKE file_tool.system_config;

-- 复制基础配置数据到测试数据库
INSERT INTO system_config (config_key, config_value, description)
SELECT config_key, config_value, description FROM file_tool.system_config; 