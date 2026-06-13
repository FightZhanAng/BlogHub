package com.blog.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.common.Result;
import com.blog.config.JwtUtil;
import com.blog.dto.CreateSeriesRequest;
import com.blog.dto.UpdateSeriesRequest;
import com.blog.entity.Series;
import com.blog.exception.BusinessException;
import com.blog.mapper.SeriesMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/series")
@RequiredArgsConstructor
@Tag(name = "文章系列", description = "系列文章管理")
public class SeriesController {

    private final SeriesMapper seriesMapper;

    private final JwtUtil jwtUtil;

    private final HttpServletRequest request;

    private Long getUserId() {
        try {
            String h = request.getHeader("Authorization");
            if (h != null && h.startsWith("Bearer ")) {
                String token = h.substring(7);
                return jwtUtil.getUserId(token);
            }
        } catch (Exception ignored) {}
        return null;
    }

    @Operation(summary = "获取所有系列")
    @GetMapping
    public Result<List<Series>> list() {
        return Result.success(seriesMapper.selectList(
                new LambdaQueryWrapper<Series>().orderByDesc(Series::getCreatedAt)));
    }

    @Operation(summary = "获取系列详情")
    @GetMapping("/{id}")
    public Result<Series> detail(@PathVariable Long id) {
        Series s = seriesMapper.selectById(id);
        if (s == null) return Result.error(404, "系列不存在");
        return Result.success(s);
    }

    @Operation(summary = "创建系列")
    @PostMapping
    public Result<Series> create(@Valid @RequestBody CreateSeriesRequest body) {
        Long userId = getUserId();
        if (userId == null) throw new BusinessException(403, "请先登录");
        Series series = new Series();
        series.setTitle(body.getTitle());
        series.setDescription(body.getDescription());
        series.setCoverImage(body.getCoverImage());
        series.setAuthorId(userId);
        series.setCreatedAt(LocalDateTime.now());
        seriesMapper.insert(series);
        return Result.success(series);
    }

    @Operation(summary = "更新系列")
    @PutMapping("/{id}")
    public Result<Series> update(@PathVariable Long id, @Valid @RequestBody UpdateSeriesRequest body) {
        Series s = seriesMapper.selectById(id);
        if (s == null) return Result.error(404, "系列不存在");
        if (body.getTitle() != null) s.setTitle(body.getTitle());
        if (body.getDescription() != null) s.setDescription(body.getDescription());
        if (body.getCoverImage() != null) s.setCoverImage(body.getCoverImage());
        seriesMapper.updateById(s);
        return Result.success(s);
    }

    @Operation(summary = "删除系列")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Long userId = getUserId();
        if (userId == null) return Result.error(401, "请先登录");
        Series s = seriesMapper.selectById(id);
        if (s == null) return Result.error(404, "系列不存在");
        if (!userId.equals(s.getAuthorId())) return Result.error(403, "无权删除");
        seriesMapper.deleteById(id);
        return Result.success();
    }
}
