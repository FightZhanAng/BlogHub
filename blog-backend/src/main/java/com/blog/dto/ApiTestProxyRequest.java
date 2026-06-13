package com.blog.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import java.util.Map;

/**
 * API 测试代理请求
 */
@Data
public class ApiTestProxyRequest {

    @NotBlank(message = "请求方法不能为空")
    private String method;

    @NotBlank(message = "请求 URL 不能为空")
    private String url;

    private Map<String, String> headers;

    private String bodyType; // json, form, multipart, raw

    private String body;
}
