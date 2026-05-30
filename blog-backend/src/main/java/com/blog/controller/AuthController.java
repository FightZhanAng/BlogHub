package com.blog.controller;

import com.blog.common.RateLimit;
import com.blog.common.Result;
import com.blog.config.JwtUtil;
import com.blog.dto.LoginRequest;
import com.blog.dto.LoginResponse;
import com.blog.dto.RefreshRequest;
import com.blog.exception.BusinessException;
import com.blog.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import lombok.RequiredArgsConstructor;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "认证管理", description = "登录注册和Token管理")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final UserService userService;

    private final JwtUtil jwtUtil;

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    @RateLimit(key = "login", max = 5, period = 60)
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(userService.login(request));
    }

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    @RateLimit(key = "register", max = 3, period = 60)
    public Result<LoginResponse> register(@Valid @RequestBody LoginRequest request) {
        return Result.success(userService.register(request));
    }

    /** 刷新 Token */
    @Operation(summary = "刷新Token")
    @PostMapping("/refresh")
    public Result<Map<String, String>> refresh(@Valid @RequestBody RefreshRequest body) {
        String refreshToken = body.getRefreshToken();
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new BusinessException(401, "Refresh Token 无效或已过期");
        }

        Long userId = jwtUtil.getUserId(refreshToken);
        String username = jwtUtil.getUsername(refreshToken);
        String role = jwtUtil.getRole(refreshToken);

        String newAccessToken = jwtUtil.generateToken(userId, username, role);
        String newRefreshToken = jwtUtil.generateRefreshToken(userId, username, role);

        log.info("Token 刷新成功: userId={}", userId);
        Map<String, String> data = new HashMap<>();
        data.put("token", newAccessToken);
        data.put("refreshToken", newRefreshToken);
        return Result.success(data);
    }
}
