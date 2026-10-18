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
        // 必须校验 type=refresh：之前只校验签名，任何人拿一个 24h 的 access token
        // 当 refreshToken 提交也能换到新 token，等于永久会话、7 天上限失效。
        if (!jwtUtil.validateToken(refreshToken, JwtUtil.TYPE_REFRESH)) {
            throw new BusinessException(401, "Refresh Token 无效或已过期");
        }

        Long userId = jwtUtil.getUserId(refreshToken);

        // 回查用户：被禁用/删除的用户不允许继续续期
        com.blog.entity.User user;
        try {
            user = userService.getUserById(userId);
        } catch (BusinessException e) {
            throw new BusinessException(401, "账号不存在或已被禁用");
        }
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(401, "账号不存在或已被禁用");
        }
        // 角色以库中最新值为准，避免降权后旧 token 仍带 admin
        String username = user.getUsername();
        String role = user.getRole() != null ? user.getRole() : jwtUtil.getRole(refreshToken);

        String newAccessToken = jwtUtil.generateToken(userId, username, role);
        String newRefreshToken = jwtUtil.generateRefreshToken(userId, username, role);

        log.info("Token 刷新成功: userId={}", userId);
        Map<String, String> data = new HashMap<>();
        data.put("token", newAccessToken);
        data.put("refreshToken", newRefreshToken);
        return Result.success(data);
    }
}
