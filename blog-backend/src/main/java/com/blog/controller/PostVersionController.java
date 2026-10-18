package com.blog.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.common.PostVisibility;
import com.blog.common.Result;
import com.blog.entity.Post;
import com.blog.entity.PostVersion;
import com.blog.mapper.PostVersionMapper;
import com.blog.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
@Tag(name = "文章版本", description = "文章版本历史")
public class PostVersionController {

    private final PostVersionMapper postVersionMapper;

    private final PostService postService;

    private final HttpServletRequest request;

    private Long currentUserId() {
        Object v = request.getAttribute("userId");
        return v instanceof Long ? (Long) v : null;
    }

    private String currentRole() {
        Object v = request.getAttribute("role");
        return v instanceof String ? (String) v : null;
    }

    /**
     * 版本历史含完整正文，必须和文章本身一样做可见性校验，
     * 否则知道 id 就能匿名读到私有/隐藏文章的全文。
     */
    private boolean readable(Long postId) {
        Post post = postService.getById(postId);
        return post != null && PostVisibility.canRead(post, currentUserId(), currentRole());
    }

    @Operation(summary = "获取文章版本列表")
    @GetMapping("/{id}/versions")
    public Result<List<PostVersion>> versions(@PathVariable Long id) {
        if (!readable(id)) {
            return Result.error(404, "文章不存在");
        }
        return Result.success(postVersionMapper.selectList(
                new LambdaQueryWrapper<PostVersion>()
                        .eq(PostVersion::getPostId, id)
                        .orderByDesc(PostVersion::getVersion)));
    }

    @Operation(summary = "获取文章指定版本")
    @GetMapping("/{id}/versions/{version}")
    public Result<PostVersion> getVersion(@PathVariable Long id, @PathVariable Integer version) {
        if (!readable(id)) {
            return Result.error(404, "文章不存在");
        }
        PostVersion pv = postVersionMapper.selectOne(
                new LambdaQueryWrapper<PostVersion>()
                        .eq(PostVersion::getPostId, id)
                        .eq(PostVersion::getVersion, version));
        if (pv == null) return Result.error(404, "版本不存在");
        return Result.success(pv);
    }
}
