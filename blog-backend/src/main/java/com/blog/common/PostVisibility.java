package com.blog.common;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.entity.Post;

/**
 * 文章可见性统一判定。
 *
 * <p>此前"私有/隐藏"只在 {@code PostController#detail} 一处判断，编辑、导出、版本历史、
 * 标签页、排行榜、相关文章等入口全部绕过。这里收敛为唯一判定入口，所有读写文章的
 * 地方都必须调用，避免再漏。</p>
 *
 * <p>规则：草稿(status=0) 只有作者本人可见；is_private 只有作者本人可见；
 * is_hidden 只有管理员可见。</p>
 */
public final class PostVisibility {

    private PostVisibility() {}

    /** 当前访问者能否读取这篇文章 */
    public static boolean canRead(Post post, Long userId, String role) {
        if (post == null) {
            return false;
        }
        boolean isAdmin = "admin".equals(role);
        boolean isOwner = userId != null && post.getAuthorId() != null && userId.equals(post.getAuthorId());

        // 草稿
        if (post.getStatus() != null && post.getStatus() == 0 && !isOwner) {
            return false;
        }
        // 仅自己可见
        if (post.getIsPrivate() != null && post.getIsPrivate() == 1 && !isOwner) {
            return false;
        }
        // 管理员隐藏
        if (post.getIsHidden() != null && post.getIsHidden() == 1 && !isAdmin) {
            return false;
        }
        return true;
    }

    /** 只允许作者本人或管理员（用于编辑、删除、版本历史等写/管理场景） */
    public static boolean canManage(Post post, Long userId, String role) {
        if (post == null) {
            return false;
        }
        if ("admin".equals(role)) {
            return true;
        }
        return userId != null && post.getAuthorId() != null && userId.equals(post.getAuthorId());
    }

    /**
     * 给列表类查询追加"当前用户可见"的 SQL 条件。
     *
     * @param wrapper 查询条件
     * @param userId  当前用户（可为 null 表示游客）
     * @param role    当前角色
     */
    public static void applyReadableFilter(LambdaQueryWrapper<Post> wrapper, Long userId, String role) {
        boolean isAdmin = "admin".equals(role);
        if (!isAdmin) {
            wrapper.eq(Post::getIsHidden, 0);
        }
        if (userId != null) {
            wrapper.apply("(is_private = 0 OR author_id = {0})", userId);
        } else {
            wrapper.eq(Post::getIsPrivate, 0);
        }
    }
}
