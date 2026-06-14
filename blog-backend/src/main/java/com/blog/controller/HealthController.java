package com.blog.controller;

import com.blog.common.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @GetMapping("/api/health")
    public Result<Map<String, Object>> health() {
        Map<String, Object> checks = new LinkedHashMap<>();
        checks.put("status", "UP");

        // MySQL
        try (Connection conn = dataSource.getConnection()) {
            checks.put("mysql", "UP");
        } catch (Exception e) {
            checks.put("mysql", "DOWN: " + e.getMessage());
            checks.put("status", "DOWN");
        }

        // Redis
        try {
            redisConnectionFactory.getConnection().ping();
            checks.put("redis", "UP");
        } catch (Exception e) {
            checks.put("redis", "DOWN: " + e.getMessage());
            checks.put("status", "DOWN");
        }

        // XXL-Job Admin
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

        return Result.success(checks);
    }
}
