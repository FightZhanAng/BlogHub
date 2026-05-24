package com.blog.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.common.Result;
import com.blog.entity.PostVersion;
import com.blog.mapper.PostVersionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
public class PostVersionController {

    @Autowired
    private PostVersionMapper postVersionMapper;

    @GetMapping("/{id}/versions")
    public Result<List<PostVersion>> versions(@PathVariable Long id) {
        return Result.success(postVersionMapper.selectList(
                new LambdaQueryWrapper<PostVersion>()
                        .eq(PostVersion::getPostId, id)
                        .orderByDesc(PostVersion::getVersion)));
    }

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
