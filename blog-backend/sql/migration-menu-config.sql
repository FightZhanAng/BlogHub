-- ============================================================
-- 菜单配置功能
-- ============================================================

-- 菜单组表
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
-- 种子数据：当前硬编码菜单
-- ============================================================

-- 菜单组
INSERT INTO menu_group (id, name, icon, sort_order, enabled, admin_only) VALUES
(1, '博客',     'Notebook',  2, 1, 0),
(2, '个人',     'User',      3, 1, 0),
(3, '发现',     'Compass',   4, 1, 0),
(4, '工具',     'SetUp',     5, 1, 0),
(5, '管理后台', 'Setting',   6, 1, 1);

-- 独立顶级项（group_id = NULL）
INSERT INTO menu_item (id, group_id, title, path, icon, sort_order, enabled, admin_only) VALUES
(1,  NULL, '首页', '/home',  'HomeFilled', 1, 1, 0),
(19, NULL, '关于', '/about', 'InfoFilled', 99, 1, 0);

-- 博客组
INSERT INTO menu_item (id, group_id, title, path, icon, sort_order, enabled, admin_only) VALUES
(2,  1, '博客列表', '/blog',     NULL, 1, 1, 0),
(3,  1, '我的文章', '/my-posts', NULL, 2, 1, 0),
(4,  1, '写文章',   '/blog/new', NULL, 3, 1, 0),
(5,  1, '标签云',   '/tags',     NULL, 4, 1, 0),
(6,  1, '文章归档', '/archive',  NULL, 5, 1, 0);

-- 个人组
INSERT INTO menu_item (id, group_id, title, path, icon, sort_order, enabled, admin_only) VALUES
(7,  2, '收藏',     '/bookmarks', NULL, 1, 1, 0),
(8,  2, '我的徽章', '/badges',    NULL, 2, 1, 0);

-- 发现组
INSERT INTO menu_item (id, group_id, title, path, icon, sort_order, enabled, admin_only) VALUES
(9,  3, '每日热点', '/hot',    NULL, 1, 1, 0),
(10, 3, '宝宝相册', '/albums', NULL, 2, 1, 0);

-- 工具组
INSERT INTO menu_item (id, group_id, title, path, icon, sort_order, enabled, admin_only) VALUES
(11, 4, 'AI 助手', '/ai-assistant', NULL, 1, 1, 0),
(12, 4, 'API 测试', '/api-tester',  NULL, 2, 1, 0);

-- 管理后台组
INSERT INTO menu_item (id, group_id, title, path, icon, sort_order, enabled, admin_only) VALUES
(13, 5, '仪表盘',   '/dashboard', NULL, 1, 1, 1),
(14, 5, '用户管理', '/users',     NULL, 2, 1, 1),
(15, 5, '评论管理', '/comments',  NULL, 3, 1, 1),
(16, 5, '图片管理', '/images',    NULL, 4, 1, 1),
(17, 5, '操作日志', '/logs',      NULL, 5, 1, 1);
