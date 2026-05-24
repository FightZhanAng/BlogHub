-- 操作日志表
CREATE TABLE IF NOT EXISTS sys_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT COMMENT '操作用户 ID',
    username VARCHAR(50) COMMENT '操作用户名',
    action VARCHAR(100) NOT NULL COMMENT '操作描述',
    resource VARCHAR(255) COMMENT '操作资源标识',
    params VARCHAR(500) COMMENT '请求参数',
    ip VARCHAR(45) COMMENT '请求 IP',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志';

-- 文章版本历史
CREATE TABLE IF NOT EXISTS post_versions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id BIGINT NOT NULL,
    title VARCHAR(255),
    content TEXT,
    version INT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_post_id (post_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章版本历史';

-- 文章系列
CREATE TABLE IF NOT EXISTS series (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    cover_image VARCHAR(500),
    author_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章系列';

-- 关注
CREATE TABLE IF NOT EXISTS follows (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    follower_id BIGINT NOT NULL COMMENT '关注者',
    following_id BIGINT NOT NULL COMMENT '被关注者',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_follow (follower_id, following_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户关注';

-- 通知
CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '接收者',
    type VARCHAR(20) NOT NULL COMMENT '类型: reply/like/follow',
    message VARCHAR(500) NOT NULL,
    related_id BIGINT COMMENT '关联资源 ID',
    is_read TINYINT(1) DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_read (user_id, is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知';

-- 文章表新增字段（使用 ALTER 避免破坏已有数据）
ALTER TABLE posts ADD COLUMN IF NOT EXISTS cover_image VARCHAR(500) AFTER author_id;
ALTER TABLE posts ADD COLUMN IF NOT EXISTS scheduled_at DATETIME AFTER cover_image;
ALTER TABLE posts ADD COLUMN IF NOT EXISTS series_id BIGINT AFTER scheduled_at;

-- 用户表新增字段
ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS bio VARCHAR(500) AFTER status;
