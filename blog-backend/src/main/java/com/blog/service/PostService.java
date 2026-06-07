package com.blog.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.blog.dto.CreatePostRequest;
import com.blog.dto.PostResponse;
import com.blog.dto.UpdatePostRequest;
import com.blog.entity.Post;

public interface PostService extends IService<Post> {

    /** 分页查询已发布的文章 */
    IPage<Post> getPublishedPosts(int page, int size, String tag, String keyword);

    /** 根据 slug 查询文章（仅已发布） */
    Post getBySlug(String slug);

    /** 根据 slug 查询文章（编辑用，允许作者查看草稿） */
    Post getBySlugForEdit(String slug, Long userId);

    /** 检查 slug 是否已被同用户占用 */
    boolean isSlugExists(String slug, Long userId, Long excludeId);

    /** 获取用户的所有 slug 列表 */
    java.util.List<String> getUserSlugs(Long userId);

    /** 增加阅读次数 */
    void incrementViews(Long id);

    /** 更新点赞数 */
    void updateLikesCount(Long id);

    /** 查询用户的文章 */
    IPage<Post> getUserPosts(Long authorId, int page, int size);

    /** 创建文章 */
    PostResponse createPost(CreatePostRequest request, Long userId);

    /** 更新文章（含权限校验） */
    PostResponse updatePost(Long id, UpdatePostRequest request, Long userId, String role);

    /** 删除文章（含权限校验） */
    void deletePost(Long id, Long userId, String role);

    /** 获取文章详情（自动 +1 阅读量） */
    PostResponse getPostDetail(String slug);

    /** 相关文章推荐 */
    java.util.List<Post> getRelatedPosts(String slug, int limit);

    /** 定时发布：扫描并发布到期的文章 */
    int publishScheduledPosts();

    /** 文章归档（按年月分组，每组含文章列表） */
    java.util.List<java.util.Map<String, Object>> getArchive();

    /** 点赞排行榜 */
    java.util.List<Post> getRankingByLikes(int limit);

    /** 全文搜索：按标题/内容/描述/标签搜索已发布文章 */
    IPage<Post> searchPosts(String query, int page, int size);
}
