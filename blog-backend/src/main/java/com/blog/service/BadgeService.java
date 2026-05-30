package com.blog.service;

import com.blog.entity.Badge;

import java.util.List;
import java.util.Map;

public interface BadgeService {

    /** 获取所有启用的徽章 */
    List<Badge> getAllBadges();

    /** 获取用户已获得的徽章 */
    List<Map<String, Object>> getUserBadges(Long userId);

    /** 标记用户徽章通知已读 */
    void markBadgesRead(Long userId);

    /** 检查并授予徽章（规则引擎入口） */
    void checkAndGrant(Long userId, String eventType);

    /** 直接授予徽章（跳过规则检查） */
    void grantBadge(Long userId, Long badgeId);
}
