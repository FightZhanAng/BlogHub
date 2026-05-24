package com.blog.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.common.Result;
import com.blog.entity.DailyViews;
import com.blog.entity.Post;
import com.blog.mapper.CommentMapper;
import com.blog.mapper.DailyViewsMapper;
import com.blog.mapper.PostMapper;
import com.blog.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 数据统计接口（仅管理员）
 */
@RestController
@RequestMapping("/api/stats")
public class StatsController {

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private com.blog.config.JwtUtil jwtUtil;

    @Autowired
    private DailyViewsMapper dailyViewsMapper;

    private boolean isAdmin(HttpServletRequest request) {
        try {
            String h = request.getHeader("Authorization");
            if (h != null && h.startsWith("Bearer ")) {
                return "admin".equals(jwtUtil.getRole(h.substring(7)));
            }
        } catch (Exception ignored) {}
        return false;
    }

    @GetMapping("/dashboard")
    public Result<Map<String, Object>> dashboard(HttpServletRequest request) {
        if (!isAdmin(request)) return Result.error(403, "无权访问");

        Map<String, Object> data = new HashMap<>();
        data.put("totalPosts", postMapper.selectCount(null));
        data.put("totalUsers", userMapper.selectCount(null));
        data.put("totalComments", commentMapper.selectCount(null));
        data.put("totalViews", postMapper.selectList(null).stream()
                .mapToInt(p -> p.getViews() != null ? p.getViews() : 0).sum());
        data.put("totalLikes", postMapper.selectList(null).stream()
                .mapToInt(p -> p.getLikes() != null ? p.getLikes() : 0).sum());

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
    @GetMapping("/search-keywords")
    public Result<List<String>> searchKeywords() {
        return Result.success(new ArrayList<>());
    }

    /** 埋点事件接收 */
    private static final Logger statsLog = LoggerFactory.getLogger("tracking");

    @PostMapping("/track")
    public Result<Void> track(@RequestBody Map<String, Object> body) {
        statsLog.info("event={} url={}", body.get("event"), body.get("url"));
        return Result.success(null);
    }
}
