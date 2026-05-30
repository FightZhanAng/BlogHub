package com.blog.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.common.Result;
import com.blog.entity.Image;
import com.blog.entity.Post;
import com.blog.mapper.ImageMapper;
import com.blog.mapper.PostMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 图片管理 — 列出/删除已上传的图片，并显示文章来源和用途
 */
@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
@Tag(name = "图片管理", description = "上传图片的管理和删除")
public class ImageController {

    private static final Logger log = LoggerFactory.getLogger(ImageController.class);

    private final ImageMapper imageMapper;

    private final PostMapper postMapper;

    @Value("${upload.dir:./uploads}")
    private String uploadDir;

    @Operation(summary = "获取图片列表")
    @GetMapping
    public Result<List<Map<String, Object>>> list() {
        List<Image> records = imageMapper.selectList(
                new LambdaQueryWrapper<Image>().orderByDesc(Image::getId));

        // 批量查询关联文章
        Set<Long> postIds = records.stream()
                .map(Image::getPostId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Post> postMap = new HashMap<>();
        if (!postIds.isEmpty()) {
            postMapper.selectBatchIds(postIds).forEach(p -> postMap.put(p.getId(), p));
        }

        List<Map<String, Object>> result = records.stream().map(img -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", img.getId());
            m.put("name", img.getFilename());
            m.put("path", img.getPath());
            m.put("size", img.getSize());
            m.put("type", img.getType() != null ? img.getType() : "unknown");
            m.put("createdAt", img.getCreatedAt());

            Post post = postMap.get(img.getPostId());
            if (post != null) {
                m.put("postId", post.getId());
                m.put("postTitle", post.getTitle());
                m.put("postSlug", post.getSlug());
            }
            return m;
        }).collect(Collectors.toList());

        return Result.success(result);
    }

    @Operation(summary = "关联图片到文章")
    @PostMapping("/link")
    public Result<Void> linkPost(@RequestParam Long imageId, @RequestParam Long postId,
                                  @RequestParam(defaultValue = "unknown") String type) {
        Image img = imageMapper.selectById(imageId);
        if (img == null) return Result.error(404, "图片不存在");
        img.setPostId(postId);
        img.setType(type);
        imageMapper.updateById(img);
        return Result.success(null);
    }

    @Operation(summary = "删除图片")
    @DeleteMapping
    public Result<Void> delete(@RequestParam String path) {
        if (path == null || !path.startsWith("/uploads/")) {
            return Result.error(400, "无效的路径");
        }
        // 删除数据库记录
        imageMapper.delete(new LambdaQueryWrapper<Image>().eq(Image::getPath, path));

        // 删除文件
        Path root = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path target = root.resolve(path.substring("/uploads/".length()).replace("/", File.separator)).normalize();
        if (!target.startsWith(root)) {
            return Result.error(400, "路径越界");
        }
        try {
            Files.deleteIfExists(target);
            log.info("图片已删除: {}", path);
            return Result.success(null);
        } catch (IOException e) {
            log.error("删除图片失败: {}", path, e);
            return Result.error(500, "删除失败");
        }
    }
}
