-- 评论通知补全：为 comments 表添加 user_id 字段
ALTER TABLE comments
ADD COLUMN user_id BIGINT NULL COMMENT '评论者用户ID（未登录可为null）' AFTER nickname;
