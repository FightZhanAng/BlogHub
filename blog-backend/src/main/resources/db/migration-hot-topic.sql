-- 每日热点表
CREATE TABLE IF NOT EXISTS hot_topics (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  platform VARCHAR(50) NOT NULL COMMENT '平台标识: github/weibo/zhihu/douyin/baidu/bilibili',
  title VARCHAR(500) NOT NULL COMMENT '热点标题',
  url VARCHAR(1000) DEFAULT '' COMMENT '跳转链接',
  hot_value VARCHAR(50) DEFAULT '' COMMENT '热度值',
  rank_num INT DEFAULT 0 COMMENT '排名',
  extra VARCHAR(500) DEFAULT '' COMMENT '额外信息(如GitHub的star数)',
  fetched_at DATE NOT NULL COMMENT '抓取日期',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_platform_date (platform, fetched_at),
  INDEX idx_fetched (fetched_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
