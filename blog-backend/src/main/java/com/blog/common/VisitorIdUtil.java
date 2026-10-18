package com.blog.common;

import javax.servlet.http.HttpServletRequest;

/**
 * 访客标识工具 — 从请求中提取唯一访客 ID
 */
public class VisitorIdUtil {

    private VisitorIdUtil() {}

    /**
     * 获取访客标识：优先使用已登录用户 ID，其次 IP
     */
    public static String getVisitorId(HttpServletRequest request) {
        return getVisitorId(request, null);
    }

    /**
     * 获取访客标识：如果传入了 userId 则用 user:{id}，否则用 IP
     */
    public static String getVisitorId(HttpServletRequest request, Long userId) {
        if (userId != null) {
            return "user:" + userId;
        }
        return "visitor:" + getClientIp(request);
    }

    /**
     * 获取客户端 IP。
     *
     * <p>X-Forwarded-For 是客户端可伪造的请求头，之前取第一个值意味着任何人换一个头
     * 就能绕过全部"游客去重"（刷赞/刷收藏/刷评论赞踩）。这里改为：
     * 优先取可信反代追加的 X-Real-IP（nginx 用 proxy_set_header 覆盖写入，客户端无法伪造），
     * 其次取 XFF 的最后一跳（由最近的可信代理追加），最后才用 remoteAddr。</p>
     */
    public static String getClientIp(HttpServletRequest request) {
        String realIp = request.getHeader("X-Real-IP");
        if (isUsable(realIp)) {
            return realIp.trim();
        }
        String xff = request.getHeader("X-Forwarded-For");
        if (isUsable(xff)) {
            String[] parts = xff.split(",");
            // 取最后一跳（可信代理追加的那一个），而不是客户端可随意填写的第一个
            return parts[parts.length - 1].trim();
        }
        String remote = request.getRemoteAddr();
        return remote != null ? remote : "unknown";
    }

    private static boolean isUsable(String ip) {
        return ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip);
    }
}
