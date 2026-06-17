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
@Tag(name = "鏂囩珷绯诲垪", description = "绯诲垪鏂囩珷绠＄悊")
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
        } catch (Exception e) { log.debug("操作失败: {}", e.getMessage()); }
        return null;
    }

    @Operation(summary = "鑾峰彇鎵€鏈夌郴鍒?)
    @GetMapping
    public Result<List<Series>> list() {
        return Result.success(seriesMapper.selectList(
                new LambdaQueryWrapper<Series>().orderByDesc(Series::getCreatedAt)));
    }

    @Operation(summary = "鑾峰彇绯诲垪璇︽儏")
    @GetMapping("/{id}")
    public Result<Series> detail(@PathVariable Long id) {
        Series s = seriesMapper.selectById(id);
        if (s == null) return Result.error(404, "绯诲垪涓嶅瓨鍦?);
        return Result.success(s);
    }

    @Operation(summary = "鍒涘缓绯诲垪")
    @PostMapping
    public Result<Series> create(@Valid @RequestBody CreateSeriesRequest body) {
        Long userId = getUserId();
        if (userId == null) throw new BusinessException(403, "璇峰厛鐧诲綍");
        Series series = new Series();
        series.setTitle(body.getTitle());
        series.setDescription(body.getDescription());
        series.setCoverImage(body.getCoverImage());
        series.setAuthorId(userId);
        series.setCreatedAt(LocalDateTime.now());
        seriesMapper.insert(series);
        return Result.success(series);
    }

    @Operation(summary = "鏇存柊绯诲垪")
    @PutMapping("/{id}")
    public Result<Series> update(@PathVariable Long id, @Valid @RequestBody UpdateSeriesRequest body) {
        Series s = seriesMapper.selectById(id);
        if (s == null) return Result.error(404, "绯诲垪涓嶅瓨鍦?);
        if (body.getTitle() != null) s.setTitle(body.getTitle());
        if (body.getDescription() != null) s.setDescription(body.getDescription());
        if (body.getCoverImage() != null) s.setCoverImage(body.getCoverImage());
        seriesMapper.updateById(s);
        return Result.success(s);
    }

    @Operation(summary = "鍒犻櫎绯诲垪")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Long userId = getUserId();
        if (userId == null) return Result.error(401, "璇峰厛鐧诲綍");
        Series s = seriesMapper.selectById(id);
        if (s == null) return Result.error(404, "绯诲垪涓嶅瓨鍦?);
        if (!userId.equals(s.getAuthorId())) return Result.error(403, "鏃犳潈鍒犻櫎");
        seriesMapper.deleteById(id);
        return Result.success();
    }
}
