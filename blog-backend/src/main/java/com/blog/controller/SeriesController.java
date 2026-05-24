package com.blog.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.common.Result;
import com.blog.config.JwtUtil;
import com.blog.entity.Series;
import com.blog.exception.BusinessException;
import com.blog.mapper.SeriesMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/series")
public class SeriesController {

    @Autowired
    private SeriesMapper seriesMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private HttpServletRequest request;

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

    @GetMapping
    public Result<List<Series>> list() {
        return Result.success(seriesMapper.selectList(
                new LambdaQueryWrapper<Series>().orderByDesc(Series::getCreatedAt)));
    }

    @GetMapping("/{id}")
    public Result<Series> detail(@PathVariable Long id) {
        Series s = seriesMapper.selectById(id);
        if (s == null) return Result.error(404, "系列不存在");
        return Result.success(s);
    }

    @PostMapping
    public Result<Series> create(@RequestBody Series series) {
        Long userId = getUserId();
        if (userId == null) throw new BusinessException(403, "请先登录");
        series.setAuthorId(userId);
        series.setCreatedAt(LocalDateTime.now());
        seriesMapper.insert(series);
        return Result.success(series);
    }

    @PutMapping("/{id}")
    public Result<Series> update(@PathVariable Long id, @RequestBody Series body) {
        Series s = seriesMapper.selectById(id);
        if (s == null) return Result.error(404, "系列不存在");
        if (body.getTitle() != null) s.setTitle(body.getTitle());
        if (body.getDescription() != null) s.setDescription(body.getDescription());
        if (body.getCoverImage() != null) s.setCoverImage(body.getCoverImage());
        seriesMapper.updateById(s);
        return Result.success(s);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        seriesMapper.deleteById(id);
        return Result.noContent();
    }
}
