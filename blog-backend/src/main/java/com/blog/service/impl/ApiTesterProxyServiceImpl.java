package com.blog.service.impl;

import com.blog.dto.ApiTestProxyRequest;
import com.blog.dto.ApiTestProxyResponse;
import com.blog.entity.ApiTestHistory;
import com.blog.exception.BusinessException;
import com.blog.mapper.ApiTestHistoryMapper;
import com.blog.service.ApiTesterProxyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URL;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApiTesterProxyServiceImpl implements ApiTesterProxyService {

    private final RestTemplate restTemplate;
    private final ApiTestHistoryMapper historyMapper;
    private final ObjectMapper objectMapper;

    // 禁止访问的内网地址
    private static final List<String> BLOCKED_HOSTS = Arrays.asList(
        "localhost", "127.0.0.1", "0.0.0.0", "::1"
    );

    @Override
    public ApiTestProxyResponse proxy(ApiTestProxyRequest request, Long userId, String role) {
        // 1. URL 安全校验（管理员跳过）
        if (!"admin".equals(role)) {
            validateUrl(request.getUrl());
        }

        // 2. 构建请求头
        HttpHeaders headers = new HttpHeaders();
        if (request.getHeaders() != null) {
            request.getHeaders().forEach(headers::set);
        }

        // 3. 构建请求体
        HttpEntity<String> entity;
        if ("GET".equalsIgnoreCase(request.getMethod())) {
            entity = new HttpEntity<>(headers);
        } else {
            String contentType = getContentType(request.getBodyType());
            if (contentType != null) {
                headers.setContentType(MediaType.parseMediaType(contentType));
            }
            entity = new HttpEntity<>(request.getBody(), headers);
        }

        // 4. 发送请求并计时
        long startTime = System.currentTimeMillis();
        ApiTestProxyResponse response = new ApiTestProxyResponse();

        try {
            HttpMethod httpMethod = HttpMethod.valueOf(request.getMethod().toUpperCase());
            ResponseEntity<String> responseEntity = restTemplate.exchange(
                request.getUrl(), httpMethod, entity, String.class
            );

            long time = System.currentTimeMillis() - startTime;

            // 5. 构建响应
            response.setStatusCode(responseEntity.getStatusCodeValue());
            response.setTime(time);

            // 响应头
            Map<String, String> responseHeaders = new LinkedHashMap<>();
            responseEntity.getHeaders().forEach((k, v) -> {
                if (!v.isEmpty()) responseHeaders.put(k, String.join(", ", v));
            });
            response.setHeaders(responseHeaders);

            // 响应体（限制 5MB）
            String body = responseEntity.getBody();
            if (body != null) {
                if (body.length() > 5 * 1024 * 1024) {
                    body = body.substring(0, 5 * 1024 * 1024) + "\n... [截断：响应体超过 5MB]";
                }
                response.setSize(body.getBytes().length);
            } else {
                response.setSize(0);
            }
            response.setBody(body);

        } catch (Exception e) {
            long time = System.currentTimeMillis() - startTime;
            response.setTime(time);
            response.setStatusCode(0);
            response.setBody("请求失败: " + e.getMessage());
            response.setSize(0);
            log.error("代理请求失败: {} {}", request.getMethod(), request.getUrl(), e);
        }

        // 6. 保存历史记录
        saveHistory(request, response, userId);

        return response;
    }

    private void validateUrl(String urlStr) {
        try {
            URL url = new URL(urlStr);
            String host = url.getHost().toLowerCase();

            // 禁止访问内网地址
            for (String blocked : BLOCKED_HOSTS) {
                if (host.equals(blocked)) {
                    throw new BusinessException(400, "禁止访问本地地址: " + host);
                }
            }

            // 禁止访问内网 IP 段
            if (host.matches("^10\\..*") || host.matches("^172\\.(1[6-9]|2[0-9]|3[01])\\..*") || host.matches("^192\\.168\\..*")) {
                throw new BusinessException(400, "禁止访问内网地址: " + host);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(400, "URL 格式无效: " + e.getMessage());
        }
    }

    private String getContentType(String bodyType) {
        if (bodyType == null) return null;
        switch (bodyType.toLowerCase()) {
            case "json": return "application/json";
            case "form": return "application/x-www-form-urlencoded";
            case "multipart": return "multipart/form-data";
            case "raw": return "text/plain";
            default: return null;
        }
    }

    private void saveHistory(ApiTestProxyRequest request, ApiTestProxyResponse response, Long userId) {
        try {
            ApiTestHistory history = new ApiTestHistory();
            history.setUserId(userId);
            history.setMethod(request.getMethod());
            history.setUrl(request.getUrl());
            if (request.getHeaders() != null) {
                history.setHeaders(objectMapper.writeValueAsString(request.getHeaders()));
            }
            history.setBodyType(request.getBodyType());
            history.setBody(request.getBody());
            history.setStatusCode(response.getStatusCode());
            history.setResponseTime(response.getTime() != null ? response.getTime().intValue() : null);
            history.setResponseSize(response.getSize());
            history.setResponseBody(response.getBody());
            if (response.getHeaders() != null) {
                history.setResponseHeaders(objectMapper.writeValueAsString(response.getHeaders()));
            }
            historyMapper.insert(history);

            // 清理旧记录，保留最近 100 条
            cleanOldHistory(userId);
        } catch (Exception e) {
            log.error("保存请求历史失败", e);
        }
    }

    private void cleanOldHistory(Long userId) {
        // 使用 SQL 删除旧记录，保留最近 100 条
        // MyBatis-Plus 不支持 LIMIT in delete，这里用简单方式
        List<ApiTestHistory> oldRecords = historyMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ApiTestHistory>()
                .eq(ApiTestHistory::getUserId, userId)
                .orderByDesc(ApiTestHistory::getCreatedAt)
                .last("LIMIT 1000")
        );
        if (oldRecords.size() > 100) {
            List<Long> idsToDelete = new ArrayList<>();
            for (int i = 100; i < oldRecords.size(); i++) {
                idsToDelete.add(oldRecords.get(i).getId());
            }
            if (!idsToDelete.isEmpty()) {
                historyMapper.deleteBatchIds(idsToDelete);
            }
        }
    }
}
