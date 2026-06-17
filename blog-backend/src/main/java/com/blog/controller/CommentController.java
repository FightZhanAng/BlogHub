package com.blog.controller;

import com.blog.common.AuditLog;
import com.blog.common.HtmlSanitizer;
import com.blog.common.PageResult;
import com.blog.common.Result;
import com.blog.config.JwtUtil;
import com.blog.dto.CommentRequest;
import com.blog.dto.ReactionRequest;
import com.blog.dto.ReactionResponse;
import com.blog.service.CommentReactionService;
import com.blog.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Tag(name = "评论管理", description = "文章评论和评论管理")
public class CommentController {

    private final CommentService commentService;

    private final CommentReactionService reactionService;

    private final JwtUtil jwtUtil;

    private final HttpServletRequest request;

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

    @Operation(summary = "获取文章评论列表")
    @GetMapping("/api/posts/{slug}/comments")
    public Result<List<Map<String, Object>>> postComments(@PathVariable String slug) {
        return Result.success(commentService.getPostComments(slug));
    }

    @Operation(summary = "添加评论")
    @PostMapping("/api/posts/{slug}/comments")
    public Result<Void> addComment(@PathVariable String slug,
                                    @Valid @RequestBody CommentRequest body) {
        String content = HtmlSanitizer.stripHtml(body.getContent());
        Long userId = getCurrentUserId();
        commentService.addComment(slug, body.getNickname(), content, body.getParentId(), userId);
        return Result.success(null);
    }

    @Operation(summary = "懒加载评论回复")
    @GetMapping("/api/comments/{id}/replies")
    public Result<PageResult<Map<String, Object>>> replies(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(commentService.getCommentReplies(id, page, size));
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

    @Operation(summary = "评论点赞/点踩")
    @PostMapping("/api/comments/{id}/reaction")
    public Result<Void> react(@PathVariable Long id, @Valid @RequestBody ReactionRequest body) {
        Long userId = getCurrentUserId();
        String ip = getClientIp();
        reactionService.react(id, userId, ip, body.getType());
        return Result.success(null);
    }

    @Operation(summary = "取消评论反应")
    @DeleteMapping("/api/comments/{id}/reaction")
    public Result<Void> removeReaction(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        String ip = getClientIp();
        reactionService.removeReaction(id, userId, ip);
        return Result.success(null);
    }

    @Operation(summary = "获取评论反应统计")
    @GetMapping("/api/comments/{id}/reactions")
    public Result<ReactionResponse> getReactions(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        String ip = getClientIp();
        return Result.success(reactionService.getReactions(id, userId, ip));
    }

    private String getClientIp() {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    // ========== 评论统计 ==========

    @Operation(summary = "获取评论统计")
    @GetMapping("/api/comments/stats")
    public Result<Map<String, Object>> stats() {
        return Result.success(commentService.getStats());
    }

    // ========== 评论管理（仅管理员） ==========

    @Operation(summary = "分页查询评论列表")
    @GetMapping("/api/comments")
    public Result<PageResult<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String nickname) {
        if (!isAdminUser()) return Result.error(403, "无权访问");
        return Result.success(commentService.getCommentList(page, size, keyword, nickname));
    }

    @Operation(summary = "删除评论")
    @DeleteMapping("/api/comments/{id}")
    @AuditLog(action = "删除评论", resource = "#id")
    public Result<Void> delete(@PathVariable Long id) {
        if (!isAdminUser()) return Result.error(403, "无权访问");
        commentService.removeById(id);
        return Result.success();
    }
}
