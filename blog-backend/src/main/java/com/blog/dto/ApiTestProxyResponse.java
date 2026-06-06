package com.blog.dto;

import lombok.Data;
import java.util.Map;

/**
 * API 测试代理响应
 */
@Data
public class ApiTestProxyResponse {

    private Integer statusCode;

    private Map<String, String> headers;

    private String body;

    private Long time; // 响应耗时(ms)

    private Integer size; // 响应大小(bytes)
}
