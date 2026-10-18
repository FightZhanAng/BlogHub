package com.blog.config;

import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

import java.util.ArrayList;
import java.util.List;

/**
 * 公开接口规则表。
 *
 * <p>规则采用「HTTP 方法 + Ant 路径」精确声明，只把真正的公开读接口暴露给匿名访客。
 * 之前的 {@code /api/posts/**} 通配放行会把文章编辑、导出、版本历史等需要鉴权的接口
 * 一并对匿名开放，这里改为逐条枚举。</p>
 *
 * <p>命中规则的请求属于「可选鉴权」：带了合法 Token 会注入 userId/role，没带或 Token
 * 非法则按游客处理，不返回 401。未命中规则的请求必须携带合法 Token。</p>
 */
public final class PublicApiRules {

    private PublicApiRules() {}

    /** 单条公开规则 */
    public static final class Rule {
        final String method;
        final String pattern;

        Rule(String method, String pattern) {
            this.method = method;
            this.pattern = pattern;
        }
    }

    private static final PathMatcher MATCHER = new AntPathMatcher();

    private static final List<Rule> RULES = new ArrayList<>();

    private static void any(String pattern) {
        for (String m : new String[]{"GET", "POST", "PUT", "DELETE"}) {
            RULES.add(new Rule(m, pattern));
        }
    }

    private static void get(String pattern) {
        RULES.add(new Rule("GET", pattern));
    }

    private static void post(String pattern) {
        RULES.add(new Rule("POST", pattern));
    }

    static {
        // ---------- 认证 ----------
        any("/api/auth/**");

        // ---------- 文章公开读 ----------
        get("/api/posts");
        get("/api/posts/search");
        get("/api/posts/archive");
        get("/api/posts/ranking/*");
        // 单段 slug：详情页
        get("/api/posts/*");
        get("/api/posts/*/related");
        // 评论（匿名可读可发）
        get("/api/posts/*/comments");
        post("/api/posts/*/comments");
        // 点赞 / 收藏（游客按 visitorId 参与）
        any("/api/posts/*/like");
        any("/api/posts/*/bookmark");

        // ---------- 收藏列表（游客按 visitorId） ----------
        get("/api/bookmarks");

        // ---------- 标签 / 系列 / 用户主页 ----------
        get("/api/tags/**");
        get("/api/series/**");
        get("/api/users/*/profile");
        get("/api/users/*/badges");

        // ---------- 统计与埋点 ----------
        get("/api/stats/**");
        post("/api/stats/track");

        // ---------- 评论反应 ----------
        get("/api/comments/stats");
        get("/api/comments/*/replies");
        get("/api/comments/*/reactions");
        post("/api/comments/*/reaction");
        RULES.add(new Rule("DELETE", "/api/comments/*/reaction"));

        // ---------- 徽章 / 热点 / 健康检查 / 菜单树 ----------
        get("/api/badges");
        get("/api/hot-topics/**");
        get("/api/health");
        get("/api/menus/tree");
    }

    /** 判断请求是否命中公开规则（即可选鉴权） */
    public static boolean isPublic(String method, String uri) {
        for (Rule rule : RULES) {
            if (rule.method.equalsIgnoreCase(method) && MATCHER.match(rule.pattern, uri)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 需要鉴权的固定字面量路径前缀。
     *
     * <p>{@code /api/posts/*} 这类单段通配会把 {@code /api/posts/my-slugs}、
     * {@code /api/posts/slug} 也一起放行，属于权限回退。这里做一次显式排除。</p>
     */
    private static final List<String> PROTECTED_LITERALS = java.util.Arrays.asList(
            "/api/posts/my-slugs",
            "/api/posts/slug"
    );

    /** 命中公开规则、且不属于必须鉴权的字面量路径 */
    public static boolean isPublicRequest(String method, String uri) {
        for (String literal : PROTECTED_LITERALS) {
            if (uri.equals(literal) || uri.startsWith(literal + "/")) {
                return false;
            }
        }
        return isPublic(method, uri);
    }
}
