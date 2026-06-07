-- ============================================================
-- 博客数据库初始化脚本（完整版）
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
    password    VARCHAR(255) NOT NULL COMMENT '密码（BCrypt）',
    nickname    VARCHAR(50)  DEFAULT '' COMMENT '昵称',
    avatar      VARCHAR(255) DEFAULT '' COMMENT '头像',
    role        VARCHAR(20)  DEFAULT 'user' COMMENT '角色: admin, user',
    status      TINYINT      DEFAULT 1 COMMENT '状态: 0=禁用, 1=启用',
    bio         VARCHAR(500) DEFAULT '' COMMENT '个人简介',
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 默认管理员（密码：admin123；算法：BCrypt）
INSERT IGNORE INTO sys_user (username, password, nickname, role) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '管理员', 'admin');

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
    is_pinned   TINYINT      DEFAULT 0 COMMENT '是否置顶: 0=否, 1=是',
    views       INT          DEFAULT 0 COMMENT '阅读次数',
    likes       INT          DEFAULT 0 COMMENT '点赞数',
    author_name VARCHAR(50)  DEFAULT '管理员' COMMENT '作者名',
    author_id   BIGINT       DEFAULT NULL COMMENT '作者用户 ID',
    cover_image VARCHAR(500) DEFAULT '' COMMENT '封面图路径',
    scheduled_at DATETIME    DEFAULT NULL COMMENT '定时发布时间',
    series_id   BIGINT       DEFAULT NULL COMMENT '系列 ID',
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    UNIQUE KEY uk_slug (slug),
    KEY idx_status (status),
    KEY idx_created_at (created_at),
    KEY idx_is_pinned (is_pinned)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章表';

-- ============================================================
-- 标签表
-- ============================================================
CREATE TABLE IF NOT EXISTS tag (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    name        VARCHAR(50)  NOT NULL COMMENT '标签显示名',
    slug        VARCHAR(100) NOT NULL COMMENT 'URL 友好标识',
    post_count  INT          DEFAULT 0 COMMENT '关联文章数',
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    UNIQUE KEY uk_slug (slug)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='标签';

-- ============================================================
-- 文章-标签关联表
-- ============================================================
CREATE TABLE IF NOT EXISTS post_tag (
    post_id     BIGINT NOT NULL COMMENT '文章 ID',
    tag_id      BIGINT NOT NULL COMMENT '标签 ID',

    PRIMARY KEY (post_id, tag_id),
    KEY idx_tag_id (tag_id),
    FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES tag(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章标签关联';

-- ============================================================
-- 评论表
-- ============================================================
CREATE TABLE IF NOT EXISTS comments (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    post_id     BIGINT       NOT NULL COMMENT '文章 ID',
    parent_id   BIGINT       DEFAULT NULL COMMENT '父评论 ID（回复）',
    nickname    VARCHAR(50)  NOT NULL COMMENT '评论者昵称',
    user_id     BIGINT       DEFAULT NULL COMMENT '评论者用户 ID（未登录可为 null）',
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

-- ============================================================
-- 评论点赞/点踩表
-- ============================================================
CREATE TABLE IF NOT EXISTS comment_reaction (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    comment_id    BIGINT       NOT NULL COMMENT '评论 ID',
    user_id       BIGINT       DEFAULT NULL COMMENT '用户 ID（游客为 NULL）',
    ip            VARCHAR(50)  DEFAULT NULL COMMENT '访客 IP',
    reaction_type TINYINT      NOT NULL COMMENT '1=赞, 0=踩',
    created_at    DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    UNIQUE KEY uk_user_comment (comment_id, user_id),
    UNIQUE KEY uk_ip_comment (comment_id, ip),
    INDEX idx_comment_id (comment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论点赞点踩表';

-- ============================================================
-- 操作日志表
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_log (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    user_id     BIGINT       COMMENT '操作用户 ID',
    username    VARCHAR(50)  COMMENT '操作用户名',
    action      VARCHAR(100) NOT NULL COMMENT '操作描述',
    resource    VARCHAR(255) COMMENT '操作资源标识',
    params      VARCHAR(500) COMMENT '请求参数',
    ip          VARCHAR(45)  COMMENT '请求 IP',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',

    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志';

-- ============================================================
-- 文章版本历史表
-- ============================================================
CREATE TABLE IF NOT EXISTS post_versions (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    post_id     BIGINT       NOT NULL COMMENT '文章 ID',
    title       VARCHAR(255) COMMENT '标题',
    content     TEXT         COMMENT '内容',
    version     INT          NOT NULL COMMENT '版本号',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    INDEX idx_post_id (post_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章版本历史';

-- ============================================================
-- 文章系列表
-- ============================================================
CREATE TABLE IF NOT EXISTS series (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    title       VARCHAR(255) NOT NULL COMMENT '系列标题',
    description TEXT         COMMENT '系列描述',
    cover_image VARCHAR(500) COMMENT '封面图',
    author_id   BIGINT       NOT NULL COMMENT '作者 ID',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章系列';

-- ============================================================
-- 用户关注表
-- ============================================================
CREATE TABLE IF NOT EXISTS follows (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    follower_id  BIGINT NOT NULL COMMENT '关注者',
    following_id BIGINT NOT NULL COMMENT '被关注者',
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    UNIQUE KEY uk_follow (follower_id, following_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户关注';

-- ============================================================
-- 通知表
-- ============================================================
CREATE TABLE IF NOT EXISTS notifications (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    user_id     BIGINT       NOT NULL COMMENT '接收者',
    type        VARCHAR(20)  NOT NULL COMMENT '类型: reply/like/follow/comment/mention',
    message     VARCHAR(500) NOT NULL COMMENT '通知内容',
    related_id  BIGINT       COMMENT '关联资源 ID',
    is_read     TINYINT(1)   DEFAULT 0 COMMENT '是否已读',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    INDEX idx_user_read (user_id, is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知';

-- ============================================================
-- 上传图片记录表
-- ============================================================
CREATE TABLE IF NOT EXISTS images (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    path        VARCHAR(500) NOT NULL COMMENT '图片相对路径',
    filename    VARCHAR(255) NOT NULL COMMENT '原始文件名',
    size        BIGINT       DEFAULT 0 COMMENT '文件大小（字节）',
    type        VARCHAR(20)  DEFAULT 'unknown' COMMENT '用途: cover/content/unknown',
    post_id     BIGINT       DEFAULT NULL COMMENT '关联文章 ID',
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    INDEX idx_path (path(255)),
    INDEX idx_post_id (post_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='上传图片记录';

-- ============================================================
-- 每日阅读量统计表
-- ============================================================
CREATE TABLE IF NOT EXISTS daily_views (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    post_id     BIGINT  NOT NULL COMMENT '文章 ID',
    date        DATE    NOT NULL COMMENT '日期',
    views       INT     DEFAULT 1 COMMENT '阅读量',

    UNIQUE KEY uk_post_date (post_id, date),
    INDEX idx_date (date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章每日阅读量';

-- ============================================================
-- 宝宝相册表
-- ============================================================
CREATE TABLE IF NOT EXISTS albums (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    title           VARCHAR(100) NOT NULL COMMENT '相册标题',
    description     VARCHAR(500) DEFAULT '' COMMENT '相册描述',
    cover_image     VARCHAR(500) DEFAULT '' COMMENT '封面图路径',
    baby_name       VARCHAR(50)  DEFAULT '' COMMENT '宝宝昵称',
    baby_birth_date DATE         DEFAULT NULL COMMENT '宝宝出生日期',
    author_id       BIGINT       NOT NULL COMMENT '创建者 ID',
    photo_count     INT          DEFAULT 0 COMMENT '照片数量',
    is_private      TINYINT(1)   DEFAULT 1 COMMENT '是否私密',
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_author (author_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='宝宝相册';

-- ============================================================
-- 照片表
-- ============================================================
CREATE TABLE IF NOT EXISTS photos (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    album_id    BIGINT       NOT NULL COMMENT '所属相册 ID',
    path        VARCHAR(500) NOT NULL COMMENT '图片路径',
    filename    VARCHAR(255) NOT NULL COMMENT '原始文件名',
    size        BIGINT       DEFAULT 0 COMMENT '文件大小',
    title       VARCHAR(200) DEFAULT '' COMMENT '照片标题',
    description VARCHAR(500) DEFAULT '' COMMENT '照片描述',
    taken_at    DATETIME     DEFAULT NULL COMMENT '拍摄时间',
    sort_order  INT          DEFAULT 0 COMMENT '排序序号',
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_album (album_id),
    INDEX idx_taken (taken_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='照片';

-- ============================================================
-- 每日热点表
-- ============================================================
CREATE TABLE IF NOT EXISTS hot_topics (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    platform    VARCHAR(50)  NOT NULL COMMENT '平台: github/weibo/zhihu/douyin/baidu/bilibili',
    title       VARCHAR(500) NOT NULL COMMENT '热点标题',
    url         VARCHAR(1000) DEFAULT '' COMMENT '跳转链接',
    hot_value   VARCHAR(50)  DEFAULT '' COMMENT '热度值',
    rank_num    INT          DEFAULT 0 COMMENT '排名',
    extra       VARCHAR(500) DEFAULT '' COMMENT '额外信息',
    fetched_at  DATE         NOT NULL COMMENT '抓取日期',
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_platform_date (platform, fetched_at),
    INDEX idx_fetched (fetched_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日热点';

-- ============================================================
-- 徽章定义表
-- ============================================================
CREATE TABLE IF NOT EXISTS badge (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    name        VARCHAR(50)  NOT NULL COMMENT '徽章名称',
    icon        VARCHAR(255) NOT NULL COMMENT '图标',
    description VARCHAR(200) NOT NULL COMMENT '描述',
    category    VARCHAR(20)  NOT NULL COMMENT '分类: interaction/content/activity/special',
    sort_order  INT          DEFAULT 0 COMMENT '排序',
    is_active   TINYINT      DEFAULT 1 COMMENT '是否启用',
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='徽章定义表';

-- ============================================================
-- 徽章规则表
-- ============================================================
CREATE TABLE IF NOT EXISTS badge_rule (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    badge_id       BIGINT       NOT NULL COMMENT '关联徽章',
    event_type     VARCHAR(50)  NOT NULL COMMENT '触发事件类型',
    condition_json TEXT         NOT NULL COMMENT '条件 JSON',
    created_at     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    FOREIGN KEY (badge_id) REFERENCES badge(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='徽章规则表';

-- ============================================================
-- 用户徽章表
-- ============================================================
CREATE TABLE IF NOT EXISTS user_badge (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    user_id   BIGINT   NOT NULL COMMENT '用户 ID',
    badge_id  BIGINT   NOT NULL COMMENT '徽章 ID',
    earned_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '获得时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_new    TINYINT  DEFAULT 1 COMMENT '是否未读',

    UNIQUE KEY uk_user_badge (user_id, badge_id),
    FOREIGN KEY (badge_id) REFERENCES badge(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户徽章表';

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

    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='API测试请求历史';

-- ============================================================
-- 菜单配置相关表
-- ============================================================

-- 菜单分组表
CREATE TABLE IF NOT EXISTS menu_group (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    name        VARCHAR(50)  NOT NULL COMMENT '分组名称',
    icon        VARCHAR(50)  DEFAULT NULL COMMENT 'Element Plus 图标名',
    sort_order  INT          NOT NULL DEFAULT 0 COMMENT '排序值（越小越靠前）',
    enabled     TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否启用 1=启用 0=禁用',
    admin_only  TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '仅管理员可见 1=是 0=否',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_sort_order (sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单分组';

-- 菜单项表
CREATE TABLE IF NOT EXISTS menu_item (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    group_id    BIGINT       DEFAULT NULL COMMENT '所属分组 ID（NULL=独立顶级项）',
    title       VARCHAR(50)  NOT NULL COMMENT '菜单标题',
    path        VARCHAR(200) NOT NULL COMMENT '路由路径',
    icon        VARCHAR(50)  DEFAULT NULL COMMENT 'Element Plus 图标名',
    sort_order  INT          NOT NULL DEFAULT 0 COMMENT '排序值（越小越靠前）',
    enabled     TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否启用 1=启用 0=禁用',
    admin_only  TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '仅管理员可见 1=是 0=否',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_group_id (group_id),
    INDEX idx_sort_order (sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单项';

-- ============================================================
-- 初始数据
-- ============================================================

-- 徽章定义
INSERT INTO badge (name, icon, description, category, sort_order) VALUES
('初次发声', '🌟', '首次发表评论', 'interaction', 1),
('评论达人', '💬', '累计评论 50 条', 'interaction', 2),
('人气王', '❤️', '收到点赞 100 次', 'interaction', 3),
('笔耕不辍', '✍️', '发布 10 篇文章', 'content', 1),
('收藏焦点', '📚', '文章被收藏 50 次', 'content', 2),
('爆款作者', '🔥', '单篇文章阅读量 1000+', 'content', 3),
('连续签到 7 天', '🔗', '连续登录 7 天', 'activity', 1),
('博龄一年', '🎂', '注册满 365 天', 'activity', 2),
('管理员', '👑', '管理员角色', 'special', 1),
('早期用户', '🌱', '注册时间早于系统上线日', 'special', 2);

-- 徽章规则
INSERT INTO badge_rule (badge_id, event_type, condition_json) VALUES
(1, 'COMMENT_CREATED', '{"metric": "total_comments", "op": ">=", "value": 1}'),
(2, 'COMMENT_CREATED', '{"metric": "total_comments", "op": ">=", "value": 50}'),
(3, 'COMMENT_LIKED', '{"metric": "total_likes_received", "op": ">=", "value": 100}'),
(4, 'POST_CREATED', '{"metric": "total_posts", "op": ">=", "value": 10}'),
(5, 'POST_BOOKMARKED', '{"metric": "total_bookmarks_received", "op": ">=", "value": 50}'),
(6, 'POST_VIEWED', '{"metric": "max_post_views", "op": ">=", "value": 1000}'),
(7, 'USER_LOGIN', '{"metric": "consecutive_login_days", "op": ">=", "value": 7}'),
(8, 'USER_REGISTERED', '{"metric": "registered_days", "op": ">=", "value": 365}');

-- 菜单分组
INSERT INTO menu_group (id, name, icon, sort_order, enabled, admin_only) VALUES
(1, '博客',     'Notebook',  2, 1, 0),
(2, '个人',     'User',      3, 1, 0),
(3, '发现',     'Compass',   4, 1, 0),
(4, '工具',     'SetUp',     5, 1, 0),
(5, '管理后台', 'Setting',   6, 1, 1);

-- 独立顶级项
INSERT INTO menu_item (id, group_id, title, path, icon, sort_order, enabled, admin_only) VALUES
(1,  NULL, '首页', '/home',  'HomeFilled', 1, 1, 0),
(19, NULL, '关于', '/about', 'InfoFilled', 99, 1, 0);

-- 博客组
INSERT INTO menu_item (id, group_id, title, path, icon, sort_order, enabled, admin_only) VALUES
(2,  1, '博客列表', '/blog',     'Reading',     1, 1, 0),
(3,  1, '我的文章', '/my-posts', 'Document',    2, 1, 0),
(4,  1, '写文章',   '/blog/new', 'EditPen',     3, 1, 0),
(5,  1, '标签云',   '/tags',     'CollectionTag', 4, 1, 0),
(6,  1, '文章归档', '/archive',  'Files',       5, 1, 0);

-- 个人组
INSERT INTO menu_item (id, group_id, title, path, icon, sort_order, enabled, admin_only) VALUES
(7,  2, '收藏',     '/bookmarks', 'Star',       1, 1, 0),
(8,  2, '我的徽章', '/badges',    'Medal',      2, 1, 0);

-- 发现组
INSERT INTO menu_item (id, group_id, title, path, icon, sort_order, enabled, admin_only) VALUES
(9,  3, '每日热点', '/hot',    'Promotion',   1, 1, 0),
(10, 3, '宝宝相册', '/albums', 'Camera',      2, 1, 0);

-- 工具组
INSERT INTO menu_item (id, group_id, title, path, icon, sort_order, enabled, admin_only) VALUES
(11, 4, 'AI 助手', '/ai-assistant', 'ChatDotRound', 1, 1, 0),
(12, 4, 'API 测试', '/api-tester',  'Monitor',      2, 1, 0);

-- 管理后台组
INSERT INTO menu_item (id, group_id, title, path, icon, sort_order, enabled, admin_only) VALUES
(13, 5, '仪表盘',   '/dashboard', 'DataLine',     1, 1, 1),
(14, 5, '用户管理', '/users',     'UserFilled',   2, 1, 1),
(15, 5, '评论管理', '/comments',  'Comment',      3, 1, 1),
(16, 5, '图片管理', '/images',    'Picture',      4, 1, 1),
(17, 5, '操作日志', '/logs',      'Operation',    5, 1, 1);
