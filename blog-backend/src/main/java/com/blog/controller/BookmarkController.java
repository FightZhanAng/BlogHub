package com.blog.controller;

import com.blog.common.Result;
import com.blog.common.VisitorIdUtil;
import com.blog.config.JwtUtil;
import com.blog.dto.PostResponse;
import com.blog.entity.Post;
import com.blog.service.BookmarkService;
import com.blog.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/bookmarks")
@RequiredArgsConstructor
@Tag(name = "收藏管理", description = "文章收藏功能")
public class BookmarkController {

    private static final Logger log = LoggerFactory.getLogger(BookmarkController.class);

    private final BookmarkService bookmarkService;

    private final PostService postService;

    private final JwtUtil jwtUtil;

    private final HttpServletRequest request;

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

    /** 获取当前用户收藏的文章列表（已登录按 user:id，未登录按 IP） */
    @Operation(summary = "获取收藏的文章列表")
    @GetMapping
    public Result<List<PostResponse>> list() {
        Long userId = getUserIdFromToken();
        String visitorId = VisitorIdUtil.getVisitorId(request, userId);
        List<Long> postIds = bookmarkService.getBookmarkedPostIds(visitorId);

        if (postIds.isEmpty()) {
            return Result.success();
        }

        List<Post> posts = postService.listByIds(postIds);
        List<PostResponse> result = posts.stream()
                .map(PostResponse::from)
                .collect(Collectors.toList());
        log.debug("访客 {} 收藏了 {} 篇文章", visitorId, result.size());
        return Result.success(result);
    }
}
