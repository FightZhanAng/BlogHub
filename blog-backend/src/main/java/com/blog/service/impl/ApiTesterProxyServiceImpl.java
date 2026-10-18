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
        // 1. URL 安全校验。
        // 之前是 if (!"admin".equals(role)) 才校验，管理员账号可直接打内网；
        // SSRF 防护属于基础设施约束，不应该因为角色就关闭。
        validateUrl(request.getUrl());

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

            // 只允许 http/https，防止 file:/gopher:/jar: 之类的协议被代理
            String protocol = url.getProtocol() == null ? "" : url.getProtocol().toLowerCase();
            if (!"http".equals(protocol) && !"https".equals(protocol)) {
                throw new BusinessException(400, "仅支持 http/https 协议");
            }

            String host = url.getHost();
            if (host == null || host.isEmpty()) {
                throw new BusinessException(400, "URL 缺少主机名");
            }
            host = host.toLowerCase();
            // URL.getHost() 对 IPv6 会带上方括号，去掉后再比较
            if (host.startsWith("[") && host.endsWith("]")) {
                host = host.substring(1, host.length() - 1);
            }

            // 解析所有 A/AAAA 记录后逐个判断，避免 2130706433、127.0.0.1.nip.io 这类绕过
            java.net.InetAddress[] addresses = java.net.InetAddress.getAllByName(host);
            if (addresses.length == 0) {
                throw new BusinessException(400, "无法解析主机: " + host);
            }
            for (java.net.InetAddress addr : addresses) {
                if (isBlocked(addr)) {
                    throw new BusinessException(400, "禁止访问内网/本地地址: " + host);
                }
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(400, "URL 格式无效: " + e.getMessage());
        }
    }

    /** 判断地址是否属于内网/保留网段 */
    private boolean isBlocked(java.net.InetAddress addr) {
        if (addr.isLoopbackAddress() || addr.isSiteLocalAddress()
                || addr.isLinkLocalAddress() || addr.isAnyLocalAddress()
                || addr.isMulticastAddress()) {
            return true;
        }
        byte[] raw = addr.getAddress();
        if (raw.length == 4) {
            int b0 = raw[0] & 0xFF;
            int b1 = raw[1] & 0xFF;
            // CGNAT 100.64/10
            if (b0 == 100 && b1 >= 64 && b1 <= 127) return true;
            // 169.254/16
            if (b0 == 169 && b1 == 254) return true;
            // 0.0.0.0/8
            if (b0 == 0) return true;
            // 127/8
            if (b0 == 127) return true;
        }
        if (raw.length == 16) {
            // IPv6 唯一本地地址 fc00::/7
            int b0 = raw[0] & 0xFF;
            if ((b0 & 0xFE) == 0xFC) return true;
        }
        return false;
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
