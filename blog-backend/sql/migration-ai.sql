-- ============================================================
-- AI 助手相关表
-- ============================================================

-- AI 对话表
CREATE TABLE IF NOT EXISTS ai_conversation (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    user_id     BIGINT       NOT NULL COMMENT '用户 ID',
    title       VARCHAR(200) NOT NULL DEFAULT '新对话' COMMENT '对话标题',
    article_id  BIGINT       DEFAULT NULL COMMENT '关联的文章 ID',
    model_id    VARCHAR(50)  NOT NULL DEFAULT 'mimo-v2.5-pro' COMMENT '使用的模型',
    thinking_enabled TINYINT(1) DEFAULT 0 COMMENT '是否开启思考模式',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_user_id (user_id),
    INDEX idx_article_id (article_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI对话表';

-- AI 消息表
CREATE TABLE IF NOT EXISTS ai_message (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    conversation_id BIGINT       NOT NULL COMMENT '对话 ID',
    role            VARCHAR(20)  NOT NULL COMMENT 'system/user/assistant',
    content         TEXT         NOT NULL COMMENT '消息内容',
    thinking_content TEXT        DEFAULT NULL COMMENT '思考过程内容',
    images          JSON         DEFAULT NULL COMMENT '图片信息',
    tokens_used     INT          DEFAULT 0 COMMENT '消耗 token 数',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_conversation_id (conversation_id),
    CONSTRAINT fk_ai_msg_conv FOREIGN KEY (conversation_id) REFERENCES ai_conversation(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI消息表';

-- AI 图片表
CREATE TABLE IF NOT EXISTS ai_image (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    user_id       BIGINT       NOT NULL COMMENT '用户 ID',
    filename      VARCHAR(255) NOT NULL COMMENT '存储文件名',
    original_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
    mime_type     VARCHAR(50)  NOT NULL COMMENT 'MIME 类型',
    size          BIGINT       NOT NULL COMMENT '文件大小（字节）',
    url           VARCHAR(500) NOT NULL COMMENT '访问 URL',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at    DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI图片表';
