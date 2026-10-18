package com.blog.controller;

import com.blog.common.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class HealthController {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    @Value("${xxl.job.admin.addresses:http://localhost:8082/xxl-job-admin}")
    private String xxlJobAdminUrl;

    /**
     * 健康检查。
     *
     * <p>这是一个公开接口（docker healthcheck 与监控用），之前会把 DB/Redis 的异常原文
     * 直接返回给任何匿名访问者（可能包含内网地址、用户名等）。现在默认只返回 UP/DOWN，
     * 只有管理员带 token 访问时才返回详细错误。</p>
     */
    @GetMapping("/api/health")
    public Result<Map<String, Object>> health(HttpServletRequest request) {
        boolean verbose = "admin".equals(request.getAttribute("role"));
        Map<String, Object> checks = new LinkedHashMap<>();
        checks.put("status", "UP");

        // MySQL
        try (Connection conn = dataSource.getConnection()) {
            checks.put("mysql", "UP");
        } catch (Exception e) {
            checks.put("mysql", verbose ? "DOWN: " + e.getMessage() : "DOWN");
            checks.put("status", "DOWN");
        }

        // Redis
        try {
            redisConnectionFactory.getConnection().ping();
            checks.put("redis", "UP");
        } catch (Exception e) {
            checks.put("redis", verbose ? "DOWN: " + e.getMessage() : "DOWN");
            checks.put("status", "DOWN");
        }

        // 外连 xxl-job 只在管理员排障时探测，避免公开接口被用来做外连放大
        if (verbose) {
            try {
                URL url = new URL(xxlJobAdminUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);
                conn.setRequestMethod("GET");
                int code = conn.getResponseCode();
                if (code == 200 || code == 302) {
                    checks.put("xxl-job", "UP");
                } else {
                    checks.put("xxl-job", "DOWN: HTTP " + code);
                    checks.put("status", "DOWN");
                }
                conn.disconnect();
            } catch (Exception e) {
                checks.put("xxl-job", "DOWN: " + e.getMessage());
                checks.put("status", "DOWN");
            }
        }

        return Result.success(checks);
    }
}
