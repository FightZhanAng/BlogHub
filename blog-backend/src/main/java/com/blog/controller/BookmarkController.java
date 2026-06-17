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
@Tag(name = "鏀惰棌绠＄悊", description = "鏂囩珷鏀惰棌鍔熻兘")
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
        } catch (Exception e) { log.debug("操作失败: {}", e.getMessage()); }
        return null;
    }

    /** 鑾峰彇褰撳墠鐢ㄦ埛鏀惰棌鐨勬枃绔犲垪琛紙宸茬櫥褰曟寜 user:id锛屾湭鐧诲綍鎸?IP锛?*/
    @Operation(summary = "鑾峰彇鏀惰棌鐨勬枃绔犲垪琛?)
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
        log.debug("璁垮 {} 鏀惰棌浜?{} 绡囨枃绔?, visitorId, result.size());
        return Result.success(result);
    }
}
