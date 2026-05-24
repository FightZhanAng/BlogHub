-- ============================================
-- 标签系统升级迁移脚本
-- ============================================

-- 0. 清理旧表（保证脚本可重跑）
DROP TABLE IF EXISTS post_tag;
DROP TABLE IF EXISTS tag;

-- 1. 创建标签表
CREATE TABLE IF NOT EXISTS tag (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL COMMENT '标签显示名',
    slug VARCHAR(100) NOT NULL COMMENT 'URL 友好标识',
    post_count INT DEFAULT 0 COMMENT '关联文章数',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_slug (slug)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='标签';

-- 2. 创建文章-标签关联表
CREATE TABLE IF NOT EXISTS post_tag (
    post_id BIGINT NOT NULL COMMENT '文章 ID',
    tag_id BIGINT NOT NULL COMMENT '标签 ID',
    PRIMARY KEY (post_id, tag_id),
    KEY idx_tag_id (tag_id),
    FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES tag(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章标签关联';

-- 3. 存量数据迁移：将 Post.tags 逗号分隔值拆分为标签记录
--    使用存储过程处理字符串拆分
DROP PROCEDURE IF EXISTS migrate_tags;
DELIMITER $$
CREATE PROCEDURE migrate_tags()
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE p_id BIGINT;
    DECLARE p_tags TEXT;
    DECLARE cur CURSOR FOR SELECT id, tags FROM posts WHERE tags IS NOT NULL AND tags != '';
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

    OPEN cur;
    read_loop: LOOP
        FETCH cur INTO p_id, p_tags;
        IF done THEN
            LEAVE read_loop;
        END IF;

        -- 按逗号拆分标签（CHAR_LENGTH 正确计算中文等多字节字符）
        SET @i = 1;
        SET @tag_count = CHAR_LENGTH(p_tags) - CHAR_LENGTH(REPLACE(p_tags, ',', '')) + 1;

        WHILE @i <= @tag_count DO
            SET @tag_name = TRIM(SUBSTRING_INDEX(SUBSTRING_INDEX(p_tags, ',', @i), ',', -1));
            -- slug：转小写，空格/点替换为连字符
            SET @tag_slug = LOWER(REPLACE(REPLACE(REPLACE(@tag_name, ' ', '-'), '.', '-'), ',', '-'));
            -- 连续多个连符合并为一个
            WHILE LOCATE('--', @tag_slug) > 0 DO
                SET @tag_slug = REPLACE(@tag_slug, '--', '-');
            END WHILE;
            -- 去掉首尾连字符
            SET @tag_slug = TRIM(BOTH '-' FROM @tag_slug);

            IF @tag_name != '' THEN
                -- 插入标签（同名标签 slug 重复时自动加后缀）
                INSERT INTO tag (name, slug) VALUES (@tag_name, @tag_slug)
                ON DUPLICATE KEY UPDATE name = VALUES(name);

                -- 获取刚插入或已存在的 tag id（通过 slug 比较避免 collation 冲突）
                SET @tag_id = (SELECT id FROM tag WHERE slug = @tag_slug COLLATE utf8mb4_unicode_ci LIMIT 1);

                -- 建立关联
                IF @tag_id IS NOT NULL THEN
                    INSERT IGNORE INTO post_tag (post_id, tag_id) VALUES (p_id, @tag_id);
                END IF;
            END IF;

            SET @i = @i + 1;
        END WHILE;
    END LOOP;
    CLOSE cur;

    -- 更新冗余计数字段
    UPDATE tag t
    SET post_count = (SELECT COUNT(*) FROM post_tag pt WHERE pt.tag_id = t.id);
END$$
DELIMITER ;

CALL migrate_tags();
DROP PROCEDURE IF EXISTS migrate_tags;
