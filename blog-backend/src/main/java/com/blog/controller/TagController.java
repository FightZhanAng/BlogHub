package com.blog.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.blog.common.PageResult;
import com.blog.common.Result;
import com.blog.dto.TagResponse;
import com.blog.entity.Post;
import com.blog.entity.Tag;
import com.blog.service.TagService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tags")
@io.swagger.v3.oas.annotations.tags.Tag(name = "标签管理", description = "标签云及标签筛选接口")
public class TagController {

    @Autowired
    private TagService tagService;

    @Operation(summary = "标签列表", description = "获取所有标签，按文章数降序")
    @GetMapping
    public Result<List<TagResponse>> list() {
        List<Tag> tags = tagService.getAllTags();
        List<TagResponse> data = tags.stream()
                .map(t -> new TagResponse(t.getId(), t.getName(), t.getSlug(), t.getPostCount()))
                .collect(Collectors.toList());
        return Result.success(data);
    }

    @Operation(summary = "标签详情", description = "按 slug 获取单个标签")
    @GetMapping("/{slug}")
    public Result<TagResponse> detail(@PathVariable String slug) {
        Tag tag = tagService.getBySlug(slug);
        if (tag == null) {
            return Result.error(404, "标签不存在");
        }
        return Result.success(new TagResponse(tag.getId(), tag.getName(), tag.getSlug(), tag.getPostCount()));
    }

    @Operation(summary = "标签下的文章", description = "分页查询该标签下的已发布文章")
    @GetMapping("/{slug}/posts")
    public Result<PageResult<Post>> posts(
            @PathVariable String slug,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        IPage<Post> postPage = tagService.getTagPosts(slug, page, size);
        return Result.success(PageResult.of(postPage));
    }
}
