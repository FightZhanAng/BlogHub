-- ============================================================
-- 修复：comments / likes / bookmarks 缺少 updated_at 列
--
-- 背景：Comment / Like / Bookmark 实体继承 BaseEntity，MyBatis-Plus 的
-- MyMetaObjectHandler 在 insert 时会填充 updatedAt 并写入 INSERT 列，
-- 而这三张表只有 created_at，导致评论、点赞、收藏接口全部 500
-- （Unknown column 'updated_at'）。
--
-- 幂等：MySQL 8 不支持 ADD COLUMN IF NOT EXISTS，这里用存储过程判断。
-- ============================================================

DROP PROCEDURE IF EXISTS blog_add_updated_at;

DELIMITER $$
CREATE PROCEDURE blog_add_updated_at()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'comments'
                     AND COLUMN_NAME = 'updated_at') THEN
        ALTER TABLE comments
            ADD COLUMN updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
            ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'likes'
                     AND COLUMN_NAME = 'updated_at') THEN
        ALTER TABLE likes
            ADD COLUMN updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
            ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bookmarks'
                     AND COLUMN_NAME = 'updated_at') THEN
        ALTER TABLE bookmarks
            ADD COLUMN updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
            ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';
    END IF;
END$$
DELIMITER ;

CALL blog_add_updated_at();

DROP PROCEDURE IF EXISTS blog_add_updated_at;

-- ============================================================
-- 清理：把"登录用户也写了 ip"的历史评论反应记录置空 ip 列，
-- 避免 uk_ip_comment(comment_id, ip) 把同一出口 IP 下的不同用户判为冲突。
-- 新代码已保证登录用户的 ip 恒为 NULL。
-- ============================================================
UPDATE comment_reaction SET ip = NULL WHERE user_id IS NOT NULL AND ip IS NOT NULL;
