-- images 表：追踪所有上传图片与文章的关联
DROP TABLE IF EXISTS `images`;
CREATE TABLE `images` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `path` VARCHAR(500) NOT NULL COMMENT '图片相对路径，如 /uploads/2026/05/uuid.png',
  `filename` VARCHAR(255) NOT NULL COMMENT '原始文件名',
  `size` BIGINT DEFAULT 0 COMMENT '文件大小（字节）',
  `type` VARCHAR(20) DEFAULT 'unknown' COMMENT '用途: cover / content / unknown',
  `post_id` BIGINT DEFAULT NULL COMMENT '关联文章 ID，可为 null',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_path` (`path`(255)),
  INDEX `idx_post_id` (`post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='上传图片记录';
