package com.blog.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.blog.common.PageResult;
import com.blog.common.Result;
import com.blog.config.JwtUtil;
import com.blog.dto.CreatePostRequest;
import com.blog.dto.PostResponse;
import com.blog.dto.UpdatePostRequest;
import com.blog.entity.Post;
import com.blog.service.PostService;
import org.slf4j.Logger;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/posts")
@Tag(name = "文章管理", description = "博客文章 CRUD 接口")
@RequiredArgsConstructor
public class PostController {

    private static final Logger log = LoggerFactory.getLogger(PostController.class);

    private final PostService postService;

    private final com.blog.service.TagService tagService;

    private final JwtUtil jwtUtil;

    private final HttpServletRequest request;

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

    private String getCurrentRole() {
        try {
            String header = request.getHeader("Authorization");
            if (header != null && header.startsWith("Bearer ")) {
                String token = header.substring(7);
                if (jwtUtil.validateToken(token)) return jwtUtil.getRole(token);
            }
        } catch (Exception ignored) {}
        return null;
    }

    @Operation(summary = "文章列表", description = "分页查询已发布的文章")
    @GetMapping
    public Result<PageResult<Post>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String keyword,
            HttpServletRequest httpRequest) {
        String role = getCurrentRole();
        Long userId = getCurrentUserId();
        return Result.success(PageResult.of(postService.getPublishedPosts(page, size, tag, keyword, role, userId)));
    }

    @Operation(summary = "文章详情")
    @GetMapping("/{slug}")
    public Result<PostResponse> detail(@PathVariable String slug) {
        Post post = postService.getBySlug(slug);
        // 私有文章仅作者可访问
        if (post.getIsPrivate() != null && post.getIsPrivate() == 1) {
            Long userId = getCurrentUserId();
            if (userId == null || !userId.equals(post.getAuthorId())) {
                return Result.error(404, "文章不存在");
            }
        }
        // 隐藏文章仅管理员可访问
        if (post.getIsHidden() != null && post.getIsHidden() == 1) {
            String role = getCurrentRole();
            if (!"admin".equals(role)) {
                return Result.error(404, "文章不存在");
            }
        }
        return Result.success(postService.getPostDetail(slug));
    }

    @Operation(summary = "获取文章（编辑用，允许草稿）")
    @GetMapping("/{slug}/edit")
    public Result<PostResponse> edit(@PathVariable String slug) {
        Long userId = getCurrentUserId();
        Post post = postService.getBySlugForEdit(slug, userId);
        return Result.success(PostResponse.from(post));
    }

    @Operation(summary = "检查 slug 是否可用")
    @GetMapping("/slug/check")
    public Result<Boolean> checkSlug(@RequestParam String slug,
                                     @RequestParam(required = false) Long excludeId) {
        Long userId = getCurrentUserId();
        boolean exists = postService.isSlugExists(slug, userId, excludeId);
        return Result.success(!exists);
    }

    @Operation(summary = "获取用户的所有 slug 列表")
    @GetMapping("/my-slugs")
    public Result<java.util.List<String>> mySlugs() {
        Long userId = getCurrentUserId();
        return Result.success(postService.getUserSlugs(userId));
    }

    @Operation(summary = "导出 Markdown")
    @GetMapping("/{slug}/export")
    public void export(@PathVariable String slug, javax.servlet.http.HttpServletResponse response) {
        Post post = postService.getBySlug(slug);
        if (post == null) {
            response.setStatus(404);
            return;
        }
        String filename = post.getSlug() + ".md";
        String content = "---\n"
            + "title: \"" + post.getTitle() + "\"\n"
            + "slug: \"" + post.getSlug() + "\"\n"
            + "date: " + (post.getCreatedAt() != null ? post.getCreatedAt().toLocalDate() : "") + "\n"
            + "tags: [" + (post.getTags() != null ? post.getTags() : "") + "]\n"
            + "---\n\n"
            + post.getContent();
        response.setContentType("text/markdown;charset=utf-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        try {
            response.getWriter().write(content);
        } catch (Exception e) {
            // ignore
        }
    }

    @Operation(summary = "创建文章")
    @PostMapping
    public Result<PostResponse> create(@Valid @RequestBody CreatePostRequest request) {
        Long userId = getCurrentUserId();
        if (userId == null) return Result.error(401, "请先登录");
        return Result.created(postService.createPost(request, userId));
    }

    @PutMapping("/{id}")
    public Result<PostResponse> update(@PathVariable Long id, @Valid @RequestBody UpdatePostRequest request) {
        Long userId = getCurrentUserId();
        if (userId == null) return Result.error(401, "请先登录");
        return Result.success(postService.updatePost(id, request, userId, getCurrentRole()));
    }

    @Operation(summary = "点赞排行榜")
    @GetMapping("/ranking/likes")
    public Result<List<Post>> rankingLikes(@RequestParam(defaultValue = "10") int limit) {
        String role = getCurrentRole();
        return Result.success(postService.getRankingByLikes(limit, role));
    }

    @Operation(summary = "全文搜索")
    @GetMapping("/search")
    public Result<IPage<Post>> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(postService.searchPosts(q, page, size));
    }

    @Operation(summary = "文章归档", description = "按年月分组的已发布文章列表")
    @GetMapping("/archive")
    public Result<List<Map<String, Object>>> archive() {
        return Result.success(postService.getArchive());
    }

    @Operation(summary = "相关文章")
    @GetMapping("/{slug}/related")
    public Result<java.util.List<Post>> related(@PathVariable String slug,
            @RequestParam(defaultValue = "4") int limit) {
        return Result.success(postService.getRelatedPosts(slug, limit));
    }

    @Operation(summary = "切换置顶（管理员）")
    @PutMapping("/{id}/pin")
    public Result<Map<String, Object>> togglePin(@PathVariable Long id) {
        String role = getCurrentRole();
        if (role == null) return Result.error(401, "请先登录");
        if (!"admin".equals(role)) return Result.error(403, "无权操作");
        Post post = postService.getById(id);
        if (post == null) return Result.error(404, "文章不存在");
        boolean pinned = post.getIsPinned() != null && post.getIsPinned() == 1;
        post.setIsPinned(pinned ? 0 : 1);
        postService.updateById(post);
        Map<String, Object> r = new java.util.HashMap<>();
        r.put("isPinned", post.getIsPinned());
        return Result.success(r);
    }

    @Operation(summary = "切换仅自己可见")
    @PutMapping("/{id}/private")
    public Result<Map<String, Object>> togglePrivate(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        if (userId == null) return Result.error(401, "请先登录");
        Post post = postService.getById(id);
        if (post == null) return Result.error(404, "文章不存在");
        if (!userId.equals(post.getAuthorId())) return Result.error(403, "只能操作自己的文章");
        boolean priv = post.getIsPrivate() != null && post.getIsPrivate() == 1;
        post.setIsPrivate(priv ? 0 : 1);
        postService.updateById(post);
        Map<String, Object> r = new java.util.HashMap<>();
        r.put("isPrivate", post.getIsPrivate());
        return Result.success(r);
    }

    @Operation(summary = "管理员隐藏文章")
    @PutMapping("/{id}/hide")
    public Result<Map<String, Object>> toggleHide(@PathVariable Long id) {
        String role = getCurrentRole();
        if (role == null) return Result.error(401, "请先登录");
        if (!"admin".equals(role)) return Result.error(403, "无权操作");
        Post post = postService.getById(id);
        if (post == null) return Result.error(404, "文章不存在");
        boolean hidden = post.getIsHidden() != null && post.getIsHidden() == 1;
        post.setIsHidden(hidden ? 0 : 1);
        postService.updateById(post);
        tagService.recalcAllPostCounts();
        Map<String, Object> r = new java.util.HashMap<>();
        r.put("isHidden", post.getIsHidden());
        return Result.success(r);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        if (userId == null) return Result.error(401, "请先登录");
        postService.deletePost(id, userId, getCurrentRole());
        return Result.success();
    }
}
