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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/posts/{slug}")
@RequiredArgsConstructor
@Tag(name = "互动功能", description = "点赞和收藏操作")
public class InteractionController {

    private static final Logger log = LoggerFactory.getLogger(InteractionController.class);

    private final PostService postService;

    private final LikeService likeService;

    private final BookmarkService bookmarkService;

    private final JwtUtil jwtUtil;

    private final NotificationMapper notificationMapper;

    private final HttpServletRequest request;

    /** 从 Authorization 头提取 userId（未登录返回 null） */
    private Long getUserIdFromToken() {
        try {
            String header = request.getHeader("Authorization");
            if (header != null && header.startsWith("Bearer ")) {
                String token = header.substring(7);
                if (jwtUtil.validateToken(token)) return jwtUtil.getUserId(token);
            }
        } catch (Exception ignored) {}
        return null;
    }

    /** 获取登录用户的 visitorId（已登录 = user:id，未登录 = visitor:ip） */
    private String getVisitorId() {
        Long userId = getUserIdFromToken();
        return VisitorIdUtil.getVisitorId(request, userId);
    }

    @Operation(summary = "切换文章点赞状态")
    @PostMapping("/like")
    public Result<Map<String, Object>> toggleLike(@PathVariable String slug) {
        Post post = postService.getBySlug(slug);
        String visitorId = getVisitorId();
        boolean liked = likeService.toggleLike(post.getId(), visitorId);
        int count = likeService.getLikeCount(post.getId());
        post.setLikes(count);
        postService.updateById(post);
        // 点赞通知
        if (liked && post.getAuthorId() != null && getUserIdFromToken() != null
                && !getUserIdFromToken().equals(post.getAuthorId())) {
            try {
                Notification n = new Notification();
                n.setUserId(post.getAuthorId());
                n.setType("like");
                n.setMessage("有人赞了你的文章《" + post.getTitle() + "》");
                n.setRelatedId(post.getId());
                n.setIsRead(0);
                n.setCreatedAt(java.time.LocalDateTime.now());
                notificationMapper.insert(n);
            } catch (Exception ignored) {}
        }
        Map<String, Object> data = new HashMap<>();
        data.put("liked", liked);
        data.put("count", count);
        return Result.success(data);
    }

    @Operation(summary = "获取文章点赞状态")
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

    @Operation(summary = "切换文章收藏状态")
    @PostMapping("/bookmark")
    public Result<Map<String, Object>> toggleBookmark(@PathVariable String slug) {
        Post post = postService.getBySlug(slug);
        String visitorId = getVisitorId();
        boolean bookmarked = bookmarkService.toggleBookmark(post.getId(), visitorId);
        Map<String, Object> data = new HashMap<>();
        data.put("bookmarked", bookmarked);
        return Result.success(data);
    }

    @Operation(summary = "获取文章收藏状态")
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
