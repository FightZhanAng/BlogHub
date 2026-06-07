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
import com.blog.service.BadgeService;
import com.blog.service.PostService;
import com.blog.service.TagService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostServiceImpl extends ServiceImpl<PostMapper, Post> implements PostService {

    private static final Logger log = LoggerFactory.getLogger(PostServiceImpl.class);

    private final PostVersionMapper postVersionMapper;

    private final ImageMapper imageMapper;

    private final DailyViewsMapper dailyViewsMapper;

    private final com.blog.mapper.UserMapper userMapper;

    private final TagService tagService;

    private final BadgeService badgeService;

    @Override
    public IPage<Post> getPublishedPosts(int page, int size, String tag, String keyword) {
        log.info("查询文章列表 page={}, size={}, tag={}, keyword={}", page, size, tag, keyword);
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<Post>()
                .eq(Post::getStatus, 1)
                .orderByDesc(Post::getIsPinned)
                .orderByDesc(Post::getCreatedAt);
        if (tag != null && !tag.trim().isEmpty()) {
            // 安全查询：先通过参数化查询获取 tagId，再关联 post_tag
            List<Long> tagIds = tagService.getTagIdsBySlugOrName(tag.trim());
            if (!tagIds.isEmpty()) {
                wrapper.inSql(Post::getId,
                    "SELECT post_id FROM post_tag WHERE tag_id IN (" +
                    tagIds.stream().map(String::valueOf).collect(Collectors.joining(",")) + ")");
            } else {
                // 无匹配标签，返回空结果
                wrapper.inSql(Post::getId, "SELECT id FROM post WHERE 1=0");
            }
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
        // 草稿文章只允许作者本人访问
        if (post.getStatus() == 0) {
            throw new ResourceNotFoundException("文章", "slug", slug);
        }
        return post;
    }

    @Override
    public Post getBySlugForEdit(String slug, Long userId) {
        log.debug("查询文章(编辑): slug={}, userId={}", slug, userId);
        Post post = baseMapper.selectOne(
                new LambdaQueryWrapper<Post>().eq(Post::getSlug, slug));
        if (post == null) {
            throw new ResourceNotFoundException("文章", "slug", slug);
        }
        // 草稿文章只允许作者本人访问
        if (post.getStatus() == 0) {
            boolean isOwner = userId != null && post.getAuthorId() != null && userId.equals(post.getAuthorId());
            if (!isOwner) {
                throw new ResourceNotFoundException("文章", "slug", slug);
            }
        }
        return post;
    }

    @Override
    public boolean isSlugExists(String slug, Long userId, Long excludeId) {
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<Post>()
                .eq(Post::getSlug, slug)
                .eq(Post::getAuthorId, userId);
        if (excludeId != null) {
            wrapper.ne(Post::getId, excludeId);
        }
        return baseMapper.selectCount(wrapper) > 0;
    }

    @Override
    public java.util.List<String> getUserSlugs(Long userId) {
        return baseMapper.selectList(
                new LambdaQueryWrapper<Post>()
                        .eq(Post::getAuthorId, userId)
                        .select(Post::getSlug)
        ).stream()
                .map(Post::getSlug)
                .filter(s -> s != null && !s.isEmpty())
                .distinct()
                .collect(java.util.stream.Collectors.toList());
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

        // 检查 slug 是否被其他用户占用
        Post slugOwner = baseMapper.selectOne(
                new LambdaQueryWrapper<Post>()
                        .eq(Post::getSlug, request.getSlug())
                        .select(Post::getAuthorId)
                        .last("LIMIT 1")
        );
        if (slugOwner != null && slugOwner.getAuthorId() != null && !slugOwner.getAuthorId().equals(userId)) {
            throw new BusinessException(400, "该 URL 标识已被其他用户占用，请更换");
        }

        // 检查是否已存在同 slug 的文章（同作者），存在则更新而非新建
        Post existing = baseMapper.selectOne(
                new LambdaQueryWrapper<Post>()
                        .eq(Post::getSlug, request.getSlug())
                        .eq(Post::getAuthorId, userId));
        if (existing != null) {
            // 更新已有草稿
            existing.setTitle(request.getTitle());
            existing.setDescription(request.getDescription());
            existing.setContent(request.getContent());
            existing.setTags(request.getTags());
            int newStatus = request.getStatus() != null ? request.getStatus() : 1;
            existing.setStatus(newStatus);
            existing.setAuthorName(request.getAuthorName());
            existing.setCoverImage(request.getCoverImage());
            existing.setSeriesId(request.getSeriesId());
            // 发布时清除定时发布时间
            if (newStatus != 2) {
                existing.setScheduledAt(null);
            } else if (request.getScheduledAt() != null && !request.getScheduledAt().isEmpty()) {
                existing.setScheduledAt(java.time.LocalDateTime.parse(request.getScheduledAt()));
            }
            baseMapper.updateById(existing);
            saveVersion(existing, getNextVersion(existing.getId()));
            tagService.updatePostTags(existing.getId(), request.getTags());
            linkPostImages(existing.getId(), request.getContent(), request.getCoverImage());
            log.info("文章更新成功: id={}, slug={}", existing.getId(), existing.getSlug());
            return PostResponse.from(existing);
        }

        // 新建文章
        Post post = new Post();
        post.setSlug(request.getSlug());
        post.setTitle(request.getTitle());
        post.setDescription(request.getDescription());
        post.setContent(request.getContent());
        post.setTags(request.getTags());
        int newStatus = request.getStatus() != null ? request.getStatus() : 1;
        post.setStatus(newStatus);
        post.setAuthorName(request.getAuthorName());
        post.setAuthorId(userId);
        post.setViews(0);
        post.setLikes(0);
        post.setCoverImage(request.getCoverImage());
        post.setSeriesId(request.getSeriesId());
        // 非定时发布时清除定时发布时间
        if (newStatus != 2) {
            post.setScheduledAt(null);
        } else if (request.getScheduledAt() != null && !request.getScheduledAt().isEmpty()) {
            post.setScheduledAt(java.time.LocalDateTime.parse(request.getScheduledAt()));
        }
        baseMapper.insert(post);
        saveVersion(post, 1);
        tagService.updatePostTags(post.getId(), request.getTags());
        linkPostImages(post.getId(), request.getContent(), request.getCoverImage());
        log.info("文章创建成功: id={}, slug={}", post.getId(), post.getSlug());

        // 检查徽章
        if (userId != null) {
            badgeService.checkAndGrant(userId, "POST_CREATED");
        }

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
        if (request.getStatus() != null) {
            post.setStatus(request.getStatus());
            // 非定时发布时清除定时发布时间
            if (request.getStatus() != 2) {
                post.setScheduledAt(null);
            }
        }
        if (request.getAuthorName() != null) post.setAuthorName(request.getAuthorName());
        if (request.getCoverImage() != null) post.setCoverImage(request.getCoverImage());
        if (request.getSeriesId() != null) post.setSeriesId(request.getSeriesId());
        if (post.getStatus() != null && post.getStatus() == 2
                && request.getScheduledAt() != null && !request.getScheduledAt().isEmpty()) {
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

        // 检查徽章（文章阅读量）
        if (post.getAuthorId() != null) {
            badgeService.checkAndGrant(post.getAuthorId(), "POST_VIEWED");
        }
        PostResponse res = PostResponse.from(post);
        // 设置作者头像
        if (post.getAuthorId() != null) {
            try {
                com.blog.entity.User u = userMapper.selectById(post.getAuthorId());
                if (u != null) res.setAuthorAvatar(u.getAvatar());
            } catch (Exception ignored) {}
        }
        return res;
    }

    @Override
    public List<Post> getRelatedPosts(String slug, int limit) {
        Post current = getBySlug(slug);
        int safeLimit = Math.max(1, Math.min(limit, 100));
        if (current.getTags() == null || current.getTags().trim().isEmpty()) {
            return baseMapper.selectList(
                    new LambdaQueryWrapper<Post>()
                            .eq(Post::getStatus, 1)
                            .ne(Post::getId, current.getId())
                            .orderByDesc(Post::getViews)
                            .last("LIMIT " + safeLimit));
        }
        // 按标签匹配（通过 post_tag 关联表），拆分逗号分隔的标签
        List<Long> tagIds = new ArrayList<>();
        for (String tag : current.getTags().split(",")) {
            tagIds.addAll(tagService.getTagIdsBySlugOrName(tag.trim()));
        }
        tagIds = tagIds.stream().distinct().collect(Collectors.toList());
        if (tagIds.isEmpty()) {
            return baseMapper.selectList(
                    new LambdaQueryWrapper<Post>()
                            .eq(Post::getStatus, 1)
                            .ne(Post::getId, current.getId())
                            .orderByDesc(Post::getViews)
                            .last("LIMIT " + safeLimit));
        }
        return baseMapper.selectList(
                new LambdaQueryWrapper<Post>()
                        .eq(Post::getStatus, 1)
                        .ne(Post::getId, current.getId())
                        .inSql(Post::getId,
                                "SELECT DISTINCT pt.post_id FROM post_tag pt WHERE pt.tag_id IN (" +
                                tagIds.stream().map(String::valueOf).collect(Collectors.joining(",")) + ")")
                        .orderByDesc(Post::getViews)
                        .last("LIMIT " + safeLimit));
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
        int safeLimit = Math.max(1, Math.min(limit, 100));
        return baseMapper.selectList(
                new LambdaQueryWrapper<Post>()
                        .eq(Post::getStatus, 1)
                        .orderByDesc(Post::getLikes)
                        .last("LIMIT " + safeLimit));
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
