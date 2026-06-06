package com.blog.controller;

import com.blog.common.Result;
import com.blog.dto.ApiTestProxyRequest;
import com.blog.dto.ApiTestProxyResponse;
import com.blog.service.ApiTesterProxyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

@RestController
@RequestMapping("/api/tester")
@RequiredArgsConstructor
@Tag(name = "API 测试工具", description = "代理请求转发")
public class ApiTesterProxyController {

    private final ApiTesterProxyService proxyService;

    @Operation(summary = "发送代理请求")
    @PostMapping("/proxy")
    public Result<ApiTestProxyResponse> proxy(@Valid @RequestBody ApiTestProxyRequest request,
                                              HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        String role = (String) httpRequest.getAttribute("role");
        return Result.success(proxyService.proxy(request, userId, role));
    }
}
