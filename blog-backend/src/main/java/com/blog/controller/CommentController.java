package com.blog.controller;

import com.blog.common.AuditLog;
import com.blog.common.HtmlSanitizer;
import com.blog.common.PageResult;
import com.blog.common.Result;
import com.blog.config.JwtUtil;
import com.blog.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
public class CommentController {

    @Autowired
    private CommentService commentService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private HttpServletRequest request;

    private boolean isAdminUser() {
        try {
            String header = request.getHeader("Authorization");
            if (header != null && header.startsWith("Bearer ")) {
                String token = header.substring(7);
                if (jwtUtil.validateToken(token)) {
                    return "admin".equals(jwtUtil.getRole(token));
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    // ========== 文章评论（公开） ==========

    @GetMapping("/api/posts/{slug}/comments")
    public Result<List<Map<String, Object>>> postComments(@PathVariable String slug) {
        return Result.success(commentService.getPostComments(slug));
    }

    @PostMapping("/api/posts/{slug}/comments")
    public Result<Void> addComment(@PathVariable String slug, @RequestBody Map<String, String> body) {
        String pid = body.get("parentId");
        Long parentId = (pid != null && !pid.isEmpty()) ? Long.parseLong(pid) : null;
        String content = HtmlSanitizer.stripHtml(body.get("content"));
        // 尝试从 token 获取 userId（未登录用户也能评论，userId=null）
        Long userId = getCurrentUserId();
        commentService.addComment(slug, body.get("nickname"), content, parentId, userId);
        return Result.success(null);
    }

    private Long getCurrentUserId() {
        try {
            String header = request.getHeader("Authorization");
            if (header != null && header.startsWith("Bearer ")) {
                String token = header.substring(7);
                if (jwtUtil.validateToken(token)) return jwtUtil.getUserId(token);
            }
        } catch (Exception ignored) {}
        return null;
    }

    // ========== 评论赞/踩（公开） ==========

    @PostMapping("/api/comments/{id}/like")
    public Result<Void> like(@PathVariable Long id) {
        commentService.likeComment(id);
        return Result.success(null);
    }

    @PostMapping("/api/comments/{id}/dislike")
    public Result<Void> dislike(@PathVariable Long id) {
        commentService.dislikeComment(id);
        return Result.success(null);
    }

    // ========== 评论统计 ==========

    @GetMapping("/api/comments/stats")
    public Result<Map<String, Object>> stats() {
        return Result.success(commentService.getStats());
    }

    // ========== 评论管理（仅管理员） ==========

    @GetMapping("/api/comments")
    public Result<PageResult<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String nickname) {
        if (!isAdminUser()) return Result.error(403, "无权访问");
        return Result.success(commentService.getCommentList(page, size, keyword, nickname));
    }

    @DeleteMapping("/api/comments/{id}")
    @AuditLog(action = "删除评论", resource = "#id")
    public Result<Void> delete(@PathVariable Long id) {
        if (!isAdminUser()) return Result.error(403, "无权访问");
        commentService.removeById(id);
        return Result.success();
    }
}
