-- 搜索历史表（用于热门搜索词统计）
CREATE TABLE IF NOT EXISTS search_history (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    keyword     VARCHAR(200) NOT NULL COMMENT '搜索关键词',
    user_id     BIGINT       DEFAULT NULL COMMENT '用户 ID（NULL=匿名）',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    INDEX idx_keyword (keyword),
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='搜索历史';
