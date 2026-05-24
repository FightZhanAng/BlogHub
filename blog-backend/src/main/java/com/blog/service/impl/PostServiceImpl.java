package com.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.blog.dto.CreatePostRequest;
import com.blog.dto.PostResponse;
import com.blog.dto.UpdatePostRequest;
import com.blog.entity.DailyViews;
import com.blog.entity.Image;
import com.blog.entity.Post;
import com.blog.mapper.DailyViewsMapper;
import com.blog.entity.PostVersion;
import com.blog.exception.BusinessException;
import com.blog.exception.ResourceNotFoundException;
import com.blog.mapper.ImageMapper;
import com.blog.mapper.PostMapper;
import com.blog.mapper.PostVersionMapper;
import com.blog.service.PostService;
import com.blog.service.TagService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PostServiceImpl extends ServiceImpl<PostMapper, Post> implements PostService {

    private static final Logger log = LoggerFactory.getLogger(PostServiceImpl.class);

    @Autowired
    private PostVersionMapper postVersionMapper;

    @Autowired
    private ImageMapper imageMapper;

    @Autowired
    private DailyViewsMapper dailyViewsMapper;

    @Autowired
    private TagService tagService;

    @Override
    public IPage<Post> getPublishedPosts(int page, int size, String tag, String keyword) {
        log.info("查询文章列表 page={}, size={}, tag={}, keyword={}", page, size, tag, keyword);
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<Post>()
                .eq(Post::getStatus, 1)
                .orderByDesc(Post::getIsPinned)
                .orderByDesc(Post::getCreatedAt);
        if (tag != null && !tag.trim().isEmpty()) {
            // 同时按 slug 和 name 匹配（兼容前端传 tag name 或 slug）
            String esc = tag.replace("'", "''");
            wrapper.inSql(Post::getId,
                    "SELECT pt.post_id FROM post_tag pt INNER JOIN tag t ON pt.tag_id = t.id WHERE t.slug = '" + esc + "' OR t.name = '" + esc + "'");
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.and(w -> w
                    .like(Post::getTitle, keyword)
                    .or().like(Post::getContent, keyword));
        }
        return baseMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public Post getBySlug(String slug) {
        log.debug("查询文章: slug={}", slug);
        Post post = baseMapper.selectOne(
                new LambdaQueryWrapper<Post>().eq(Post::getSlug, slug));
        if (post == null) {
            throw new ResourceNotFoundException("文章", "slug", slug);
        }
        return post;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void incrementViews(Long id) {
        log.debug("增加阅读量: postId={}", id);
        baseMapper.update(null, new LambdaUpdateWrapper<Post>()
                .setSql("views = views + 1")
                .eq(Post::getId, id));
        // 记录每日阅读量
        try {
            java.time.LocalDate today = java.time.LocalDate.now();
            DailyViews dv = dailyViewsMapper.selectOne(
                    new LambdaQueryWrapper<DailyViews>()
                            .eq(DailyViews::getPostId, id)
                            .eq(DailyViews::getDate, today));
            if (dv != null) {
                dv.setViews(dv.getViews() + 1);
                dailyViewsMapper.updateById(dv);
            } else {
                dv = new DailyViews();
                dv.setPostId(id);
                dv.setDate(today);
                dv.setViews(1);
                dailyViewsMapper.insert(dv);
            }
        } catch (Exception e) {
            log.warn("记录每日阅读量失败: {}", e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateLikesCount(Long id) {
        log.debug("更新点赞数: postId={}", id);
        int count = baseMapper.selectCountLikes(id);
        baseMapper.update(null, new LambdaUpdateWrapper<Post>()
                .set(Post::getLikes, count)
                .eq(Post::getId, id));
    }

    @Override
    public IPage<Post> getUserPosts(Long authorId, int page, int size) {
        return baseMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Post>()
                        .eq(Post::getAuthorId, authorId)
                        .orderByDesc(Post::getCreatedAt));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PostResponse createPost(CreatePostRequest request, Long userId) {
        log.info("创建文章: title={}, authorId={}", request.getTitle(), userId);
        Post post = new Post();
        post.setSlug(request.getSlug());
        post.setTitle(request.getTitle());
        post.setDescription(request.getDescription());
        post.setContent(request.getContent());
        post.setTags(request.getTags());
        post.setStatus(request.getStatus() != null ? request.getStatus() : 1);
        post.setAuthorName(request.getAuthorName());
        post.setAuthorId(userId);
        post.setViews(0);
        post.setLikes(0);
        post.setCoverImage(request.getCoverImage());
        post.setSeriesId(request.getSeriesId());
        if (request.getScheduledAt() != null && !request.getScheduledAt().isEmpty()) {
            post.setScheduledAt(java.time.LocalDateTime.parse(request.getScheduledAt()));
        } else {
            post.setScheduledAt(null);
        }
        baseMapper.insert(post);
        saveVersion(post, 1);
        // 处理标签关联
        tagService.updatePostTags(post.getId(), request.getTags());
        // 关联图片
        linkPostImages(post.getId(), request.getContent(), request.getCoverImage());
        log.info("文章创建成功: id={}, slug={}", post.getId(), post.getSlug());
        return PostResponse.from(post);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PostResponse updatePost(Long id, UpdatePostRequest request, Long userId, String role) {
        log.info("更新文章: id={}, userId={}", id, userId);
        Post post = baseMapper.selectById(id);
        if (post == null) {
            throw new BusinessException(404, "文章不存在");
        }

        // 权限校验
        boolean isAdmin = "admin".equals(role);
        boolean isOwner = userId != null && post.getAuthorId() != null && userId.equals(post.getAuthorId());
        if (!isOwner && !isAdmin) {
            throw new BusinessException(403, "无权修改此文章");
        }

        if (request.getSlug() != null) post.setSlug(request.getSlug());
        if (request.getTitle() != null) post.setTitle(request.getTitle());
        if (request.getDescription() != null) post.setDescription(request.getDescription());
        if (request.getContent() != null) post.setContent(request.getContent());
        if (request.getTags() != null) post.setTags(request.getTags());
        if (request.getStatus() != null) post.setStatus(request.getStatus());
        if (request.getAuthorName() != null) post.setAuthorName(request.getAuthorName());
        if (request.getCoverImage() != null) post.setCoverImage(request.getCoverImage());
        if (request.getSeriesId() != null) post.setSeriesId(request.getSeriesId());
        if (request.getScheduledAt() != null && !request.getScheduledAt().isEmpty()) {
            post.setScheduledAt(java.time.LocalDateTime.parse(request.getScheduledAt()));
        }
        baseMapper.updateById(post);
        saveVersion(post, getNextVersion(id));
        // 更新标签关联
        tagService.updatePostTags(post.getId(), request.getTags());
        // 关联图片
        linkPostImages(post.getId(), request.getContent(), request.getCoverImage());
        log.info("文章更新成功: id={}", id);
        return PostResponse.from(post);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePost(Long id, Long userId, String role) {
        log.info("删除文章: id={}, userId={}", id, userId);
        Post post = baseMapper.selectById(id);
        if (post == null) {
            throw new BusinessException(404, "文章不存在");
        }

        boolean isAdmin = "admin".equals(role);
        boolean isOwner = userId != null && post.getAuthorId() != null && userId.equals(post.getAuthorId());
        if (!isOwner && !isAdmin) {
            throw new BusinessException(403, "无权删除此文章");
        }

        baseMapper.deleteById(id);
        log.info("文章删除成功: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PostResponse getPostDetail(String slug) {
        Post post = getBySlug(slug);
        incrementViews(post.getId());
        post.setViews(post.getViews() + 1);
        log.info("查看文章: slug={}, views={}", slug, post.getViews());
        return PostResponse.from(post);
    }

    @Override
    public List<Post> getRelatedPosts(String slug, int limit) {
        Post current = getBySlug(slug);
        if (current.getTags() == null || current.getTags().trim().isEmpty()) {
            return baseMapper.selectList(
                    new LambdaQueryWrapper<Post>()
                            .eq(Post::getStatus, 1)
                            .ne(Post::getId, current.getId())
                            .orderByDesc(Post::getViews)
                            .last("LIMIT " + limit));
        }
        // 按标签匹配（通过 post_tag 关联表）
        return baseMapper.selectList(
                new LambdaQueryWrapper<Post>()
                        .eq(Post::getStatus, 1)
                        .ne(Post::getId, current.getId())
                        .inSql(Post::getId,
                                "SELECT DISTINCT pt.post_id FROM post_tag pt WHERE pt.tag_id IN (" +
                                "SELECT t.id FROM tag t INNER JOIN post_tag pt2 ON t.id = pt2.tag_id WHERE pt2.post_id = " + current.getId() + ")" )
                        .orderByDesc(Post::getViews)
                        .last("LIMIT " + limit));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Scheduled(fixedRate = 60000)
    public int publishScheduledPosts() {
        int count = baseMapper.update(null, new LambdaUpdateWrapper<Post>()
                .set(Post::getStatus, 1)
                .eq(Post::getStatus, 0)
                .isNotNull(Post::getScheduledAt)
                .le(Post::getScheduledAt, LocalDateTime.now()));
        if (count > 0) {
            log.info("定时发布: {} 篇文章已发布", count);
        }
        return count;
    }

    @Override
    public List<Post> getRankingByLikes(int limit) {
        return baseMapper.selectList(
                new LambdaQueryWrapper<Post>()
                        .eq(Post::getStatus, 1)
                        .orderByDesc(Post::getLikes)
                        .last("LIMIT " + limit));
    }

    @Override
    public IPage<Post> searchPosts(String query, int page, int size) {
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<Post>()
                .eq(Post::getStatus, 1);
        if (query != null && !query.trim().isEmpty()) {
            String q = query.trim();
            wrapper.and(w -> w
                .like(Post::getTitle, q)
                .or().like(Post::getContent, q)
                .or().like(Post::getDescription, q)
            );
        }
        wrapper.orderByDesc(Post::getLikes);
        return baseMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public List<Map<String, Object>> getArchive() {
        List<Post> posts = baseMapper.selectList(
                new LambdaQueryWrapper<Post>()
                        .eq(Post::getStatus, 1)
                        .orderByDesc(Post::getCreatedAt)
                        .select(Post::getId, Post::getTitle, Post::getSlug, Post::getCreatedAt));
        // 按年月分组
        Map<String, List<Map<String, Object>>> grouped = new LinkedHashMap<>();
        for (Post p : posts) {
            String key = p.getCreatedAt().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"));
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("title", p.getTitle());
            item.put("slug", p.getSlug());
            item.put("date", p.getCreatedAt().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(item);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, List<Map<String, Object>>> entry : grouped.entrySet()) {
            Map<String, Object> group = new LinkedHashMap<>();
            group.put("month", entry.getKey());
            group.put("posts", entry.getValue());
            result.add(group);
        }
        return result;
    }

    /** 保存版本历史 */
    private void saveVersion(Post post, int version) {
        try {
            PostVersion pv = new PostVersion();
            pv.setPostId(post.getId());
            pv.setTitle(post.getTitle());
            pv.setContent(post.getContent());
            pv.setVersion(version);
            pv.setCreatedAt(LocalDateTime.now());
            postVersionMapper.insert(pv);
            // 超过50个版本时清理最旧的
            long count = postVersionMapper.selectCount(
                    new LambdaQueryWrapper<PostVersion>().eq(PostVersion::getPostId, post.getId()));
            if (count > 50) {
                postVersionMapper.delete(
                        new LambdaQueryWrapper<PostVersion>()
                                .eq(PostVersion::getPostId, post.getId())
                                .orderByAsc(PostVersion::getVersion)
                                .last("LIMIT " + (count - 50)));
            }
        } catch (Exception e) {
            log.warn("版本保存失败: postId={}", post.getId(), e);
        }
    }

    /** 获取下一个版本号 */
    private int getNextVersion(Long postId) {
        PostVersion last = postVersionMapper.selectOne(
                new LambdaQueryWrapper<PostVersion>()
                        .eq(PostVersion::getPostId, postId)
                        .orderByDesc(PostVersion::getVersion)
                        .last("LIMIT 1"));
        return (last != null ? last.getVersion() : 0) + 1;
    }

    /** 扫描文章内容中的图片并建立关联 */
    private void linkPostImages(Long postId, String content, String coverImage) {
        try {
            // 封面图
            if (coverImage != null && !coverImage.isEmpty()) {
                String path = coverImage;
                if (path.startsWith("http")) {
                    try { path = new java.net.URL(path).getPath(); } catch (Exception ignored) {}
                }
                imageMapper.update(null,
                        new LambdaUpdateWrapper<Image>()
                                .set(Image::getPostId, postId)
                                .set(Image::getType, "cover")
                                .eq(Image::getPath, path));
            }
            // 正文图片
            if (content != null && !content.isEmpty()) {
                String clean = content.replaceAll("https?://[^/]+", "");
                java.util.regex.Matcher m =
                    java.util.regex.Pattern.compile("/uploads/[^\\s)\"'\\]\\[]>]+")
                        .matcher(clean);
                while (m.find()) {
                    String imgPath = m.group();
                    imageMapper.update(null,
                            new LambdaUpdateWrapper<Image>()
                                    .set(Image::getPostId, postId)
                                    .set(Image::getType, "content")
                                    .eq(Image::getPath, imgPath));
                }
            }
        } catch (Exception e) {
            log.warn("关联图片失败: {}", e.getMessage());
        }
    }
}
