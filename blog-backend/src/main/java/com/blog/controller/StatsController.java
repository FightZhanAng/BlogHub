package com.blog.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.common.Result;
import com.blog.dto.TrackRequest;
import com.blog.entity.DailyViews;
import com.blog.entity.Post;
import com.blog.mapper.CommentMapper;
import com.blog.mapper.DailyViewsMapper;
import com.blog.mapper.PostMapper;
import com.blog.mapper.TagMapper;
import com.blog.mapper.UserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.time.LocalDate;
import java.util.*;

/**
 * 数据统计接口（仅管理员）
 */
@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
@Tag(name = "数据统计", description = "仪表盘和埋点统计")
public class StatsController {

    private final PostMapper postMapper;

    private final UserMapper userMapper;

    private final CommentMapper commentMapper;

    private final com.blog.config.JwtUtil jwtUtil;

    private final DailyViewsMapper dailyViewsMapper;

    private final TagMapper tagMapper;

    private boolean isAdmin(HttpServletRequest request) {
        try {
            String h = request.getHeader("Authorization");
            if (h != null && h.startsWith("Bearer ")) {
                return "admin".equals(jwtUtil.getRole(h.substring(7)));
            }
        } catch (Exception ignored) {}
        return false;
    }

    @Operation(summary = "获取仪表盘数据")
    @GetMapping("/dashboard")
    public Result<Map<String, Object>> dashboard(HttpServletRequest request) {
        if (!isAdmin(request)) return Result.error(403, "无权访问");

        Map<String, Object> data = new HashMap<>();
        data.put("totalPosts", postMapper.selectCount(null));
        data.put("totalUsers", userMapper.selectCount(null));
        data.put("totalComments", commentMapper.selectCount(null));
        data.put("totalViews", postMapper.selectMaps(
                new LambdaQueryWrapper<Post>().select("COALESCE(SUM(views), 0) AS total"))
                .get(0).get("total"));
        data.put("totalLikes", postMapper.selectMaps(
                new LambdaQueryWrapper<Post>().select("COALESCE(SUM(likes), 0) AS total"))
                .get(0).get("total"));

        List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate d = LocalDate.now().minusDays(i);
            Map<String, Object> point = new HashMap<>();
            point.put("date", d.toString());
            point.put("posts", postMapper.selectCount(
                    new LambdaQueryWrapper<Post>()
                            .apply("DATE(created_at) = {0}", d)));
            trend.add(point);
        }
        data.put("recentTrend", trend);
        return Result.success(data);
    }

    /** 文章阅读趋势 */
    @Operation(summary = "获取文章阅读趋势")
    @GetMapping("/post/{postId}/trend")
    public Result<List<Map<String, Object>>> postTrend(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "7") int days) {
        LocalDate start = LocalDate.now().minusDays(days - 1);
        List<DailyViews> records = dailyViewsMapper.selectList(
                new LambdaQueryWrapper<DailyViews>()
                        .eq(DailyViews::getPostId, postId)
                        .ge(DailyViews::getDate, start)
                        .orderByAsc(DailyViews::getDate));
        Map<LocalDate, Integer> map = new HashMap<>();
        for (DailyViews r : records) map.put(r.getDate(), r.getViews());
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            LocalDate d = start.plusDays(i);
            Map<String, Object> point = new HashMap<>();
            point.put("date", d.toString());
            point.put("views", map.getOrDefault(d, 0));
            result.add(point);
        }
        return Result.success(result);
    }

    /** 热门搜索词 */
    @Operation(summary = "获取热门搜索词")
    @GetMapping("/search-keywords")
    public Result<List<String>> searchKeywords() {
        List<String> keywords = tagMapper.selectList(
                new LambdaQueryWrapper<com.blog.entity.Tag>()
                        .orderByDesc(com.blog.entity.Tag::getPostCount)
                        .last("LIMIT 10"))
                .stream()
                .map(com.blog.entity.Tag::getName)
                .collect(java.util.stream.Collectors.toList());
        return Result.success(keywords);
    }

    /** 埋点事件接收 */
    private static final Logger statsLog = LoggerFactory.getLogger("tracking");

    @Operation(summary = "接收埋点事件")
    @PostMapping("/track")
    public Result<Void> track(@Valid @RequestBody TrackRequest body) {
        statsLog.info("event={} url={} visitorId={}", body.getEvent(), body.getUrl(), body.getVisitorId());
        return Result.success(null);
    }
}
