-- ============================================================
-- 博客数据库初始化脚本
-- 使用方式: mysql -u root -p < sql/init.sql
-- ============================================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS blog_db
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE blog_db;

-- ============================================================
-- 用户表
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_user (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    username    VARCHAR(50)  NOT NULL COMMENT '用户名',
    password    VARCHAR(255) NOT NULL COMMENT '密码（MD5 + salt）',
    nickname    VARCHAR(50)  DEFAULT '' COMMENT '昵称',
    avatar      VARCHAR(255) DEFAULT '' COMMENT '头像',
    role        VARCHAR(20)  DEFAULT 'user' COMMENT '角色: admin, user',
    status      TINYINT      DEFAULT 1 COMMENT '状态: 0=禁用, 1=启用',
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 默认管理员（密码：admin123；算法：MD5(password + "BlogHub@2024")）
INSERT IGNORE INTO sys_user (username, password, nickname, role) VALUES
('admin', '6c924bda1a099506fd567ad3bf9c8964', '管理员', 'admin');

-- ============================================================
-- 文章表
-- ============================================================
CREATE TABLE IF NOT EXISTS posts (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    slug        VARCHAR(128) NOT NULL COMMENT 'URL 友好标识',
    title       VARCHAR(255) NOT NULL COMMENT '文章标题',
    description VARCHAR(500) DEFAULT '' COMMENT '文章摘要',
    content     MEDIUMTEXT   COMMENT 'Markdown 正文',
    tags        VARCHAR(255) DEFAULT '' COMMENT '标签（逗号分隔）',
    status      TINYINT      DEFAULT 1 COMMENT '状态: 0=草稿, 1=已发布',
    views       INT          DEFAULT 0 COMMENT '阅读次数',
    likes       INT          DEFAULT 0 COMMENT '点赞数',
    author_name VARCHAR(50)  DEFAULT '管理员' COMMENT '作者名',
    author_id   BIGINT       DEFAULT NULL COMMENT '作者用户 ID',
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    UNIQUE KEY uk_slug (slug),
    KEY idx_status (status),
    KEY idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章表';

-- ============================================================
-- 评论表（支持回复 + 赞/踩）
-- ============================================================
CREATE TABLE IF NOT EXISTS comments (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    post_id     BIGINT       NOT NULL COMMENT '文章 ID',
    parent_id   BIGINT       DEFAULT NULL COMMENT '父评论 ID（回复）',
    nickname    VARCHAR(50)  NOT NULL COMMENT '评论者昵称',
    content     VARCHAR(500) NOT NULL COMMENT '评论内容',
    likes       INT          DEFAULT 0 COMMENT '点赞数',
    dislikes    INT          DEFAULT 0 COMMENT '踩数',
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    KEY idx_post_id (post_id),
    KEY idx_parent_id (parent_id),
    CONSTRAINT fk_comment_post FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论表';

-- ============================================================
-- 点赞表
-- ============================================================
CREATE TABLE IF NOT EXISTS likes (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    post_id     BIGINT       NOT NULL COMMENT '文章 ID',
    visitor_id  VARCHAR(64)  NOT NULL COMMENT '访客标识（IP 或设备 ID）',
    liked       TINYINT      DEFAULT 1 COMMENT '1=已赞, 0=取消',
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    UNIQUE KEY uk_post_visitor (post_id, visitor_id),
    CONSTRAINT fk_like_post FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='点赞表';

-- ============================================================
-- 收藏表
-- ============================================================
CREATE TABLE IF NOT EXISTS bookmarks (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    post_id     BIGINT       NOT NULL COMMENT '文章 ID',
    visitor_id  VARCHAR(64)  NOT NULL COMMENT '访客标识',
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    UNIQUE KEY uk_bookmark (post_id, visitor_id),
    CONSTRAINT fk_bookmark_post FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收藏表';
