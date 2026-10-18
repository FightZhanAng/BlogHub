package com.blog.config;

import com.blog.common.Result;
import com.blog.common.ResultCode;
import com.blog.common.RequireAdmin;
import com.blog.entity.User;
import com.blog.mapper.UserMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 认证拦截器 — 校验请求头中的 Token。
 *
 * <p>放行策略由 {@link PublicApiRules} 精确声明：命中公开规则的请求属于「可选鉴权」
 * （Token 合法则注入身份，非法/缺失按游客处理）；未命中的请求必须携带合法 Token，
 * 失败时返回真正的 401/403 状态码，而不是 HTTP 200 + body code。</p>
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(AuthInterceptor.class);

    private final JwtUtil jwtUtil;

    private final UserMapper userMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // OPTIONS 请求直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String uri = request.getRequestURI();
        boolean optional = PublicApiRules.isPublicRequest(request.getMethod(), uri);

        String authHeader = request.getHeader("Authorization");
        String token = (authHeader != null && authHeader.startsWith("Bearer "))
                ? authHeader.substring(7) : null;

        boolean authenticated = false;
        if (token != null && jwtUtil.validateToken(token)) {
            try {
                Long userId = jwtUtil.getUserId(token);
                String role = jwtUtil.getRole(token);

                // 回查用户状态：被禁用/删除的用户即使 Token 未过期也立即失效
                User user = userMapper.selectById(userId);
                if (user == null || user.getStatus() == null || user.getStatus() != 1) {
                    if (!optional) {
                        writeError(response, HttpServletResponse.SC_UNAUTHORIZED,
                                ResultCode.UNAUTHORIZED, "账号不存在或已被禁用");
                        return false;
                    }
                } else {
                    request.setAttribute("userId", userId);
                    request.setAttribute("username", user.getUsername());
                    request.setAttribute("role", user.getRole() != null ? user.getRole() : role);
                    authenticated = true;
                }
            } catch (Exception e) {
                log.debug("Token 解析失败: {}", e.getMessage());
            }
        }

        // 公开接口：允许匿名继续
        if (optional) {
            return true;
        }

        if (!authenticated) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED,
                    ResultCode.UNAUTHORIZED, "未登录或登录已过期");
            return false;
        }

        // 管理员接口校验（@RequireAdmin 可标在方法或整个 Controller 上）
        if (requiresAdmin(handler)) {
            String role = (String) request.getAttribute("role");
            if (!"admin".equals(role)) {
                writeError(response, HttpServletResponse.SC_FORBIDDEN,
                        ResultCode.FORBIDDEN, "需要管理员权限");
                return false;
            }
        }

        return true;
    }

    private boolean requiresAdmin(Object handler) {
        if (!(handler instanceof HandlerMethod)) {
            return false;
        }
        HandlerMethod hm = (HandlerMethod) handler;
        if (hm.getMethodAnnotation(RequireAdmin.class) != null) {
            return true;
        }
        return hm.getBeanType().isAnnotationPresent(RequireAdmin.class);
    }

    private void writeError(HttpServletResponse response, int status,
                            ResultCode code, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        String json = objectMapper.writeValueAsString(Result.error(code, message));
        response.getOutputStream().write(json.getBytes(StandardCharsets.UTF_8));
    }
}
