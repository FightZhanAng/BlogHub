package com.blog.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.common.Result;
import com.blog.entity.PostVersion;
import com.blog.mapper.PostVersionMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
@Tag(name = "文章版本", description = "文章版本历史")
public class PostVersionController {

    private final PostVersionMapper postVersionMapper;

    @Operation(summary = "获取文章版本列表")
    @GetMapping("/{id}/versions")
    public Result<List<PostVersion>> versions(@PathVariable Long id) {
        return Result.success(postVersionMapper.selectList(
                new LambdaQueryWrapper<PostVersion>()
                        .eq(PostVersion::getPostId, id)
                        .orderByDesc(PostVersion::getVersion)));
    }

    @Operation(summary = "获取文章指定版本")
    @GetMapping("/{id}/versions/{version}")
    public Result<PostVersion> getVersion(@PathVariable Long id, @PathVariable Integer version) {
        PostVersion pv = postVersionMapper.selectOne(
                new LambdaQueryWrapper<PostVersion>()
                        .eq(PostVersion::getPostId, id)
                        .eq(PostVersion::getVersion, version));
        if (pv == null) return Result.error(404, "版本不存在");
        return Result.success(pv);
    }
}
