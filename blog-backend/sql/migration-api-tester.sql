-- ============================================================
-- API 测试工具相关表
-- ============================================================

-- 环境变量表
CREATE TABLE IF NOT EXISTS api_test_env (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    user_id     BIGINT       NOT NULL COMMENT '用户 ID',
    name        VARCHAR(100) NOT NULL COMMENT '变量名',
    value       VARCHAR(2000) NOT NULL COMMENT '变量值',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='API测试环境变量';

-- 请求集合表
CREATE TABLE IF NOT EXISTS api_test_collection (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    name        VARCHAR(100) NOT NULL COMMENT '集合名称',
    description VARCHAR(500) DEFAULT '' COMMENT '集合描述',
    user_id     BIGINT       NOT NULL COMMENT '用户 ID',
    base_url    VARCHAR(500) DEFAULT '' COMMENT '默认 Base URL',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='API测试请求集合';

-- 保存的请求表
CREATE TABLE IF NOT EXISTS api_test_request (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    collection_id   BIGINT       DEFAULT NULL COMMENT '所属集合 ID',
    user_id         BIGINT       NOT NULL COMMENT '用户 ID',
    name            VARCHAR(100) NOT NULL COMMENT '请求名称',
    method          VARCHAR(10)  NOT NULL DEFAULT 'GET' COMMENT 'HTTP 方法',
    url             VARCHAR(2000) NOT NULL COMMENT '请求 URL',
    headers         TEXT         DEFAULT NULL COMMENT '请求头 JSON',
    body_type       VARCHAR(20)  DEFAULT NULL COMMENT 'Body 类型: json/form/multipart/raw',
    body            MEDIUMTEXT   DEFAULT NULL COMMENT '请求体内容',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_user_id (user_id),
    INDEX idx_collection_id (collection_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='API测试保存的请求';

-- 请求历史表
CREATE TABLE IF NOT EXISTS api_test_history (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    user_id         BIGINT       NOT NULL COMMENT '用户 ID',
    method          VARCHAR(10)  NOT NULL COMMENT 'HTTP 方法',
    url             VARCHAR(2000) NOT NULL COMMENT '请求 URL',
    headers         TEXT         DEFAULT NULL COMMENT '请求头 JSON',
    body_type       VARCHAR(20)  DEFAULT NULL COMMENT 'Body 类型',
    body            MEDIUMTEXT   DEFAULT NULL COMMENT '请求体',
    status_code     INT          DEFAULT NULL COMMENT '响应状态码',
    response_time   INT          DEFAULT NULL COMMENT '响应耗时(ms)',
    response_size   INT          DEFAULT NULL COMMENT '响应大小(bytes)',
    response_body   MEDIUMTEXT   DEFAULT NULL COMMENT '响应体',
    response_headers TEXT        DEFAULT NULL COMMENT '响应头 JSON',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='API测试请求历史';
