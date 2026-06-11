-- 文章可见性：仅自己可见 + 管理员隐藏
ALTER TABLE posts
    ADD COLUMN is_private TINYINT(1) NOT NULL DEFAULT 0 COMMENT '仅自己可见: 0=否, 1=是' AFTER is_pinned,
    ADD COLUMN is_hidden TINYINT(1) NOT NULL DEFAULT 0 COMMENT '管理员隐藏: 0=否, 1=是' AFTER is_private;
