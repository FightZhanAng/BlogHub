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
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return "visitor:" + ip;
    }
}
