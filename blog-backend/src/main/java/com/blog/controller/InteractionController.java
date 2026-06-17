package com.blog.controller;

import com.blog.common.Result;
import com.blog.common.VisitorIdUtil;
import com.blog.config.JwtUtil;
import com.blog.entity.Notification;
import com.blog.entity.Post;
import com.blog.mapper.NotificationMapper;
import com.blog.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/posts/{slug}")
@RequiredArgsConstructor
@Tag(name = "浜掑姩鍔熻兘", description = "鐐硅禐鍜屾敹钘忔搷浣?)
public class InteractionController {

    private final PostService postService;

    private final LikeService likeService;

    private final BookmarkService bookmarkService;

    private final BadgeService badgeService;

    private final JwtUtil jwtUtil;

    private final NotificationMapper notificationMapper;

    private final HttpServletRequest request;

    /** 浠?Authorization 澶存彁鍙?userId锛堟湭鐧诲綍杩斿洖 null锛?*/
    private Long getUserIdFromToken() {
        try {
            String header = request.getHeader("Authorization");
            if (header != null && header.startsWith("Bearer ")) {
                String token = header.substring(7);
                if (jwtUtil.validateToken(token)) return jwtUtil.getUserId(token);
            }
        } catch (Exception e) { log.debug("操作失败: {}", e.getMessage()); }
        return null;
    }

    /** 鑾峰彇鐧诲綍鐢ㄦ埛鐨?visitorId锛堝凡鐧诲綍 = user:id锛屾湭鐧诲綍 = visitor:ip锛?*/
    private String getVisitorId() {
        Long userId = getUserIdFromToken();
        return VisitorIdUtil.getVisitorId(request, userId);
    }

    @Operation(summary = "鍒囨崲鏂囩珷鐐硅禐鐘舵€?)
    @PostMapping("/like")
    public Result<Map<String, Object>> toggleLike(@PathVariable String slug) {
        Post post = postService.getBySlug(slug);
        String visitorId = getVisitorId();
        boolean liked = likeService.toggleLike(post.getId(), visitorId);
        int count = likeService.getLikeCount(post.getId());
        post.setLikes(count);
        postService.updateById(post);
        // 鐐硅禐閫氱煡
        if (liked && post.getAuthorId() != null && getUserIdFromToken() != null
                && !getUserIdFromToken().equals(post.getAuthorId())) {
            try {
                Notification n = new Notification();
                n.setUserId(post.getAuthorId());
                n.setType("like");
                n.setMessage("鏈変汉璧炰簡浣犵殑鏂囩珷銆? + post.getTitle() + "銆?);
                n.setRelatedId(post.getId());
                n.setIsRead(0);
                n.setCreatedAt(java.time.LocalDateTime.now());
                notificationMapper.insert(n);
            } catch (Exception e) { log.debug("操作失败: {}", e.getMessage()); }
        }
        Map<String, Object> data = new HashMap<>();
        data.put("liked", liked);
        data.put("count", count);

        // 妫€鏌ュ窘绔狅紙鏂囩珷琚偣璧烇級
        if (liked && post.getAuthorId() != null) {
            badgeService.checkAndGrant(post.getAuthorId(), "POST_LIKED");
        }

        return Result.success(data);
    }

    @Operation(summary = "鑾峰彇鏂囩珷鐐硅禐鐘舵€?)
    @GetMapping("/like")
    public Result<Map<String, Object>> getLikeStatus(@PathVariable String slug) {
        Post post = postService.getBySlug(slug);
        String visitorId = getVisitorId();
        boolean liked = likeService.isLiked(post.getId(), visitorId);
        int count = likeService.getLikeCount(post.getId());
        Map<String, Object> data = new HashMap<>();
        data.put("liked", liked);
        data.put("count", count);
        return Result.success(data);
    }

    @Operation(summary = "鍒囨崲鏂囩珷鏀惰棌鐘舵€?)
    @PostMapping("/bookmark")
    public Result<Map<String, Object>> toggleBookmark(@PathVariable String slug) {
        Post post = postService.getBySlug(slug);
        String visitorId = getVisitorId();
        boolean bookmarked = bookmarkService.toggleBookmark(post.getId(), visitorId);
        Map<String, Object> data = new HashMap<>();
        data.put("bookmarked", bookmarked);

        // 妫€鏌ュ窘绔狅紙鏂囩珷琚敹钘忥級
        if (bookmarked && post.getAuthorId() != null) {
            badgeService.checkAndGrant(post.getAuthorId(), "POST_BOOKMARKED");
        }

        return Result.success(data);
    }

    @Operation(summary = "鑾峰彇鏂囩珷鏀惰棌鐘舵€?)
    @GetMapping("/bookmark")
    public Result<Map<String, Object>> getBookmarkStatus(@PathVariable String slug) {
        Post post = postService.getBySlug(slug);
        String visitorId = getVisitorId();
        boolean bookmarked = bookmarkService.isBookmarked(post.getId(), visitorId);
        Map<String, Object> data = new HashMap<>();
        data.put("bookmarked", bookmarked);
        return Result.success(data);
    }
}
