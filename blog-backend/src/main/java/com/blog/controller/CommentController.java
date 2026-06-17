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
@Tag(name = "璇勮绠＄悊", description = "鏂囩珷璇勮鍜岃瘎璁虹鐞?)
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
        } catch (Exception e) { log.debug("操作失败: {}", e.getMessage()); }
        return false;
    }

    // ========== 鏂囩珷璇勮锛堝叕寮€锛?==========

    @Operation(summary = "鑾峰彇鏂囩珷璇勮鍒楄〃")
    @GetMapping("/api/posts/{slug}/comments")
    public Result<List<Map<String, Object>>> postComments(@PathVariable String slug) {
        return Result.success(commentService.getPostComments(slug));
    }

    @Operation(summary = "娣诲姞璇勮")
    @PostMapping("/api/posts/{slug}/comments")
    public Result<Void> addComment(@PathVariable String slug,
                                    @Valid @RequestBody CommentRequest body) {
        String content = HtmlSanitizer.stripHtml(body.getContent());
        String nickname = HtmlSanitizer.stripHtml(body.getNickname());
        Long userId = getCurrentUserId();
        commentService.addComment(slug, nickname, content, body.getParentId(), userId);
        return Result.success(null);
    }

    @Operation(summary = "鎳掑姞杞借瘎璁哄洖澶?)
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
        } catch (Exception e) { log.debug("操作失败: {}", e.getMessage()); }
        return null;
    }

    // ========== 璇勮璧?韪╋紙鍏紑锛?==========

    @Operation(summary = "璇勮鐐硅禐/鐐硅俯")
    @PostMapping("/api/comments/{id}/reaction")
    public Result<Void> react(@PathVariable Long id, @Valid @RequestBody ReactionRequest body) {
        Long userId = getCurrentUserId();
        String ip = getClientIp();
        reactionService.react(id, userId, ip, body.getType());
        return Result.success(null);
    }

    @Operation(summary = "鍙栨秷璇勮鍙嶅簲")
    @DeleteMapping("/api/comments/{id}/reaction")
    public Result<Void> removeReaction(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        String ip = getClientIp();
        reactionService.removeReaction(id, userId, ip);
        return Result.success(null);
    }

    @Operation(summary = "鑾峰彇璇勮鍙嶅簲缁熻")
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

    // ========== 璇勮缁熻 ==========

    @Operation(summary = "鑾峰彇璇勮缁熻")
    @GetMapping("/api/comments/stats")
    public Result<Map<String, Object>> stats() {
        return Result.success(commentService.getStats());
    }

    // ========== 璇勮绠＄悊锛堜粎绠＄悊鍛橈級 ==========

    @Operation(summary = "鍒嗛〉鏌ヨ璇勮鍒楄〃")
    @GetMapping("/api/comments")
    public Result<PageResult<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String nickname) {
        if (!isAdminUser()) return Result.error(403, "鏃犳潈璁块棶");
        return Result.success(commentService.getCommentList(page, size, keyword, nickname));
    }

    @Operation(summary = "鍒犻櫎璇勮")
    @DeleteMapping("/api/comments/{id}")
    @AuditLog(action = "鍒犻櫎璇勮", resource = "#id")
    public Result<Void> delete(@PathVariable Long id) {
        if (!isAdminUser()) return Result.error(403, "鏃犳潈璁块棶");
        commentService.removeById(id);
        return Result.success();
    }
}
