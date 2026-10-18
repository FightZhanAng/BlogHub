package com.blog.common;

/**
 * 分页参数规范化。
 *
 * <p>之前各 Controller 的 {@code size} 完全没有上限，{@code GET /api/posts?size=1000000}
 * 会一次性把全表拉进内存（Post 还带 MEDIUMTEXT 正文）。这里统一收敛。</p>
 */
public final class PageParams {

    private PageParams() {}

    public static final int MAX_SIZE = 100;
    public static final int DEFAULT_SIZE = 10;

    public static int page(int page) {
        return Math.max(1, page);
    }

    public static int size(int size) {
        if (size <= 0) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }
}
