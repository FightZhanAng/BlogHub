package com.blog.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.common.Result;
import com.blog.config.JwtUtil;
import com.blog.dto.FollowRequest;
import com.blog.entity.Follow;
import com.blog.entity.Notification;
import com.blog.entity.Post;
import com.blog.mapper.FollowMapper;
import com.blog.mapper.NotificationMapper;
import com.blog.mapper.PostMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "绀句氦鍔熻兘", description = "鍏虫敞銆侀€氱煡鍜屾帓琛屾")
public class SocialController {

    private final com.blog.mapper.UserMapper userMapper;

    private final FollowMapper followMapper;

    private final NotificationMapper notificationMapper;

    private final PostMapper postMapper;

    private final JwtUtil jwtUtil;

    private final HttpServletRequest request;

    private Long getUserId() {
        try {
            String h = request.getHeader("Authorization");
            if (h != null && h.startsWith("Bearer ")) {
                return jwtUtil.getUserId(h.substring(7));
            }
        } catch (Exception e) { log.debug("操作失败: {}", e.getMessage()); }
        return null;
    }

    private String getUsername() {
        try {
            String h = request.getHeader("Authorization");
            if (h != null && h.startsWith("Bearer ")) {
                return jwtUtil.getUsername(h.substring(7));
            }
        } catch (Exception e) { log.debug("操作失败: {}", e.getMessage()); }
        return null;
    }

    // ==================== 鍏虫敞 ====================

    @Operation(summary = "鍏虫敞鐢ㄦ埛")
    @PostMapping("/follows")
    public Result<Void> follow(@Valid @RequestBody FollowRequest body) {
        Long userId = getUserId();
        if (userId == null) return Result.error(403, "璇峰厛鐧诲綍");
        Long followingId = body.getUserId();
        if (userId.equals(followingId)) return Result.error(400, "涓嶈兘鍏虫敞鑷繁");
        Follow f = new Follow();
        f.setFollowerId(userId);
        f.setFollowingId(followingId);
        f.setCreatedAt(LocalDateTime.now());
        try {
            followMapper.insert(f);
            // 鍏虫敞閫氱煡
            Notification n = new Notification();
            n.setUserId(followingId);
            n.setType("follow");
            n.setRelatedId(userId);
            n.setIsRead(0);
            n.setCreatedAt(LocalDateTime.now());
            String username = getUsername();
            n.setMessage((username != null ? username : "鏈変汉") + "鍏虫敞浜嗕綘");
            notificationMapper.insert(n);
        } catch (Exception e) { log.debug("操作失败: {}", e.getMessage()); }
        return Result.success(null);
    }

    @Operation(summary = "鍙栨秷鍏虫敞")
    @DeleteMapping("/follows/{userId}")
    public Result<Void> unfollow(@PathVariable Long userId) {
        Long cur = getUserId();
        if (cur == null) return Result.error(403, "璇峰厛鐧诲綍");
        followMapper.delete(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getFollowerId, cur)
                .eq(Follow::getFollowingId, userId));
        return Result.success(null);
    }

    @Operation(summary = "鑾峰彇鐢ㄦ埛涓婚〉淇℃伅")
    @GetMapping("/users/{id}/profile")
    public Result<Map<String, Object>> profile(@PathVariable Long id) {
        Map<String, Object> data = new HashMap<>();
        data.put("userId", id);
        // 鐢ㄦ埛鍩烘湰淇℃伅
        com.blog.entity.User user = userMapper.selectById(id);
        if (user != null) {
            data.put("username", user.getUsername());
            data.put("nickname", user.getNickname());
            data.put("avatar", user.getAvatar());
            data.put("bio", user.getBio());
            data.put("role", user.getRole());
        }
        data.put("followerCount", followMapper.selectCount(
                new LambdaQueryWrapper<Follow>().eq(Follow::getFollowingId, id)));
        data.put("followingCount", followMapper.selectCount(
                new LambdaQueryWrapper<Follow>().eq(Follow::getFollowerId, id)));
        Long cur = getUserId();
        if (cur != null) {
            data.put("isFollowing", followMapper.selectCount(
                    new LambdaQueryWrapper<Follow>()
                            .eq(Follow::getFollowerId, cur)
                            .eq(Follow::getFollowingId, id)) > 0);
        }
        return Result.success(data);
    }

    // ==================== 閫氱煡 ====================

    @Operation(summary = "鑾峰彇閫氱煡鍒楄〃")
    @GetMapping("/notifications")
    public Result<IPage<Notification>> notifications(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = getUserId();
        if (userId == null) return Result.error(403, "璇峰厛鐧诲綍");
        return Result.success(notificationMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, userId)
                        .orderByDesc(Notification::getCreatedAt)));
    }

    @Operation(summary = "鑾峰彇鏈閫氱煡鏁?)
    @GetMapping("/notifications/unread-count")
    public Result<Map<String, Long>> unreadCount() {
        Long userId = getUserId();
        if (userId == null) {
            Map<String, Long> m = new HashMap<>();
            m.put("count", 0L);
            return Result.success(m);
        }
        long count = notificationMapper.selectCount(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, userId)
                        .eq(Notification::getIsRead, 0));
        Map<String, Long> r = new HashMap<>();
        r.put("count", count);
        return Result.success(r);
    }

    @Operation(summary = "鏍囪鍏ㄩ儴閫氱煡宸茶")
    @PutMapping("/notifications/read-all")
    public Result<Void> readAll() {
        Long userId = getUserId();
        if (userId != null) {
            notificationMapper.update(null, new LambdaUpdateWrapper<Notification>()
                    .eq(Notification::getUserId, userId)
                    .set(Notification::getIsRead, 1));
        }
        return Result.success();
    }

    // ==================== 鎺掕姒?====================

    @Operation(summary = "鑾峰彇鏂囩珷鎺掕姒?)
    @GetMapping("/posts/ranking")
    public Result<List<Post>> ranking() {
        List<Post> top = postMapper.selectList(
                new LambdaQueryWrapper<Post>()
                        .eq(Post::getStatus, 1)
                        .orderByDesc(Post::getViews)
                        .last("LIMIT 10"));
        return Result.success(top);
    }
}
