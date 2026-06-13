package com.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.entity.*;
import com.blog.mapper.*;
import com.blog.service.BadgeService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BadgeServiceImpl implements BadgeService {

    private static final Logger log = LoggerFactory.getLogger(BadgeServiceImpl.class);

    private final BadgeMapper badgeMapper;
    private final BadgeRuleMapper badgeRuleMapper;
    private final UserBadgeMapper userBadgeMapper;
    private final CommentMapper commentMapper;
    private final PostMapper postMapper;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;

    @Override
    public List<Badge> getAllBadges() {
        return badgeMapper.selectList(
                new LambdaQueryWrapper<Badge>()
                        .eq(Badge::getIsActive, 1)
                        .orderByAsc(Badge::getCategory, Badge::getSortOrder));
    }

    @Override
    public List<Map<String, Object>> getUserBadges(Long userId) {
        List<Badge> allBadges = getAllBadges();
        Set<Long> earnedBadgeIds = userBadgeMapper.selectList(
                new LambdaQueryWrapper<UserBadge>()
                        .eq(UserBadge::getUserId, userId))
                .stream().map(UserBadge::getBadgeId).collect(Collectors.toSet());

        return allBadges.stream().map(b -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", b.getId());
            m.put("name", b.getName());
            m.put("icon", b.getIcon());
            m.put("description", b.getDescription());
            m.put("category", b.getCategory());
            m.put("earned", earnedBadgeIds.contains(b.getId()));
            return m;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void markBadgesRead(Long userId) {
        List<UserBadge> unread = userBadgeMapper.selectList(
                new LambdaQueryWrapper<UserBadge>()
                        .eq(UserBadge::getUserId, userId)
                        .eq(UserBadge::getIsNew, 1));
        for (UserBadge ub : unread) {
            ub.setIsNew(0);
            userBadgeMapper.updateById(ub);
        }
    }

    @Override
    @Transactional
    public void checkAndGrant(Long userId, String eventType) {
        if (userId == null) return;

        // 查询该事件类型对应的所有规则
        List<BadgeRule> rules = badgeRuleMapper.selectList(
                new LambdaQueryWrapper<BadgeRule>()
                        .eq(BadgeRule::getEventType, eventType));
        if (rules.isEmpty()) return;

        // 查询用户已获得的徽章
        Set<Long> earnedBadgeIds = userBadgeMapper.selectList(
                new LambdaQueryWrapper<UserBadge>()
                        .eq(UserBadge::getUserId, userId))
                .stream().map(UserBadge::getBadgeId).collect(Collectors.toSet());

        for (BadgeRule rule : rules) {
            // 跳过已获得的徽章
            if (earnedBadgeIds.contains(rule.getBadgeId())) continue;

            try {
                JsonNode condition = objectMapper.readTree(rule.getConditionJson());
                String metric = condition.get("metric").asText();
                String op = condition.get("op").asText();
                long value = condition.get("value").asLong();

                long currentValue = getMetricValue(userId, metric);
                if (evaluateCondition(currentValue, op, value)) {
                    grantBadge(userId, rule.getBadgeId());
                }
            } catch (Exception e) {
                log.error("检查徽章规则失败: badgeId={}, error={}", rule.getBadgeId(), e.getMessage());
            }
        }
    }

    private long getMetricValue(Long userId, String metric) {
        switch (metric) {
            case "total_comments":
                return commentMapper.selectCount(
                        new LambdaQueryWrapper<Comment>()
                                .eq(Comment::getUserId, userId));

            case "total_likes_received":
                // 统计用户评论收到的点赞数
                List<Comment> userComments = commentMapper.selectList(
                        new LambdaQueryWrapper<Comment>()
                                .eq(Comment::getUserId, userId));
                return userComments.stream()
                        .mapToLong(c -> c.getLikes() != null ? c.getLikes() : 0)
                        .sum();

            case "total_posts":
                return postMapper.selectCount(
                        new LambdaQueryWrapper<Post>()
                                .eq(Post::getAuthorId, userId));

            case "total_bookmarks_received":
                // 统计用户文章被收藏数（需要 bookmark 表）
                // 暂时返回 0，后续可以扩展
                return 0;

            case "max_post_views":
                List<Post> userPosts = postMapper.selectList(
                        new LambdaQueryWrapper<Post>()
                                .eq(Post::getAuthorId, userId));
                return userPosts.stream()
                        .mapToLong(p -> p.getViews() != null ? p.getViews() : 0)
                        .max()
                        .orElse(0);

            case "consecutive_login_days":
                User user = userMapper.selectById(userId);
                if (user == null) return 0;
                // 简化实现：根据注册时间计算活跃天数
                // 实际应该用登录记录表
                return LocalDate.now().toEpochDay() - user.getCreatedAt().toLocalDate().toEpochDay();

            case "registered_days":
                User registeredUser = userMapper.selectById(userId);
                if (registeredUser == null) return 0;
                return LocalDate.now().toEpochDay() - registeredUser.getCreatedAt().toLocalDate().toEpochDay();

            default:
                return 0;
        }
    }

    private boolean evaluateCondition(long current, String op, long target) {
        switch (op) {
            case ">=": return current >= target;
            case "<=": return current <= target;
            case "=": return current == target;
            case ">": return current > target;
            case "<": return current < target;
            default: return false;
        }
    }

    @Transactional
    public void grantBadge(Long userId, Long badgeId) {
        // 检查是否已获得
        Long count = userBadgeMapper.selectCount(
                new LambdaQueryWrapper<UserBadge>()
                        .eq(UserBadge::getUserId, userId)
                        .eq(UserBadge::getBadgeId, badgeId));
        if (count > 0) return;

        UserBadge userBadge = new UserBadge();
        userBadge.setUserId(userId);
        userBadge.setBadgeId(badgeId);
        userBadge.setIsNew(1);
        userBadgeMapper.insert(userBadge);

        Badge badge = badgeMapper.selectById(badgeId);
        log.info("用户获得徽章: userId={}, badge={}", userId, badge != null ? badge.getName() : badgeId);
    }
}
