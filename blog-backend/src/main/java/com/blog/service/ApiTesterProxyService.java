package com.blog.service;

import com.blog.dto.ApiTestProxyRequest;
import com.blog.dto.ApiTestProxyResponse;

public interface ApiTesterProxyService {
    ApiTestProxyResponse proxy(ApiTestProxyRequest request, Long userId, String role);
}
