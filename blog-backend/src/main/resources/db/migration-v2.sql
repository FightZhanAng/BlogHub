-- 1. 文章置顶
ALTER TABLE posts ADD COLUMN `is_pinned` TINYINT DEFAULT 0 COMMENT '是否置顶 0=否 1=是' AFTER `status`;
CREATE INDEX idx_is_pinned ON posts(`is_pinned`);

-- 2. 每日阅读量统计
DROP TABLE IF EXISTS `daily_views`;
CREATE TABLE `daily_views` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `post_id` BIGINT NOT NULL,
  `date` DATE NOT NULL,
  `views` INT DEFAULT 1,
  UNIQUE KEY `uk_post_date` (`post_id`, `date`),
  INDEX `idx_date` (`date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章每日阅读量';
