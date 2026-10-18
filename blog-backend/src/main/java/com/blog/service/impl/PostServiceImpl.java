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
import com.xxl.job.core.handler.annotation.XxlJob;
import com.xxl.job.core.context.XxlJobHelper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
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
    public IPage<Post> getPublishedPosts(int page, int size, String tag, String keyword, String role, Long userId) {
        log.info("查询文章列表 page={}, size={}, tag={}, keyword={}, role={}, userId={}", page, size, tag, keyword, role, userId);
        boolean isAdmin = "admin".equals(role);
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<Post>()
                .eq(Post::getStatus, 1)
                .orderByDesc(Post::getIsPinned)
                .orderByDesc(Post::getCreatedAt);
        // 管理员可以看到隐藏文章，普通用户看不到
        if (!isAdmin) {
            wrapper.eq(Post::getIsHidden, 0);
        }
        // 私有文章：仅作者本人可见
        if (userId != null) {
            wrapper.apply("(is_private = 0 OR author_id = {0})", userId);
        } else {
            wrapper.eq(Post::getIsPrivate, 0);
        }
        if (tag != null && !tag.trim().isEmpty()) {
            // 安全查询：先通过参数化查询获取 tagId，再关联 post_tag
            List<Long> tagIds = tagService.getTagIdsBySlugOrName(tag.trim());
            if (!tagIds.isEmpty()) {
                wrapper.inSql(Post::getId,
                    "SELECT post_id FROM post_tag WHERE tag_id IN (" +
                    tagIds.stream().map(String::valueOf).collect(Collectors.joining(",")) + ")");
            } else {
                // 无匹配标签：直接返回空结果（不能写 "SELECT id FROM post ..."，
                // 表名是 posts，写错会抛 Table doesn't exist 变成 500）
                wrapper.apply("1 = 0");
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
            // 草稿(count=0) 与不存在对外一视同仁
            throw new ResourceNotFoundException("文章", "slug", slug);
        }
        if (post.getStatus() != null && post.getStatus() == 0) {
            throw new ResourceNotFoundException("文章", "slug", slug);
        }
        return post;
    }

    @Override
    public Post getBySlugForEdit(String slug, Long userId, String role) {
        log.debug("查询文章(编辑): slug={}, userId={}", slug, userId);
        Post post = baseMapper.selectOne(
                new LambdaQueryWrapper<Post>().eq(Post::getSlug, slug));
        if (post == null) {
            throw new ResourceNotFoundException("文章", "slug", slug);
        }
        // 编辑入口需要管理权限：作者本人或管理员，其余一律 404（不泄露存在性）
        if (!com.blog.common.PostVisibility.canManage(post, userId, role)) {
            throw new ResourceNotFoundException("文章", "slug", slug);
        }
        return post;
    }

    @Override
    public boolean isSlugExists(String slug, Long userId, Long excludeId) {
        // slug 在库中是全局唯一（uk_slug），这里必须全局判断，
        // 否则两个作者都会通过校验，最终在 insert 时抛 Duplicate entry。
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<Post>()
                .eq(Post::getSlug, slug);
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
                        .eq(Post::getIsHidden, 0)
                        .orderByDesc(Post::getCreatedAt));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PostResponse createPost(CreatePostRequest request, Long userId) {
        log.info("创建文章: title={}, authorId={}", request.getTitle(), userId);

        int newStatus = normalizeStatus(request.getStatus());
        java.time.LocalDateTime scheduledAt = parseScheduledAt(newStatus, request.getScheduledAt());

        // slug 在库中全局唯一（uk_slug），先按全局查
        Post existing = baseMapper.selectOne(
                new LambdaQueryWrapper<Post>()
                        .eq(Post::getSlug, request.getSlug())
                        .last("LIMIT 1"));

        if (existing != null) {
            // 别人的文章：一律拒绝，不泄露归属细节
            if (existing.getAuthorId() == null || !existing.getAuthorId().equals(userId)) {
                throw new BusinessException(400, "该 URL 标识已被占用，请更换");
            }
            // 只有草稿允许"以创建的形式继续保存"；已发布/定时发布必须走显式更新，
            // 否则用户从 slug 下拉里选到一篇已发布文章，一点"保存草稿"就会覆盖线上正文。
            if (existing.getStatus() != null && existing.getStatus() != 0) {
                throw new BusinessException(409,
                        "该 URL 标识已存在已发布文章，请从编辑页修改，不能以新建方式覆盖");
            }
            existing.setTitle(request.getTitle());
            existing.setDescription(request.getDescription());
            existing.setContent(request.getContent());
            existing.setTags(request.getTags());
            existing.setStatus(newStatus);
            existing.setAuthorName(request.getAuthorName());
            existing.setCoverImage(request.getCoverImage());
            existing.setSeriesId(request.getSeriesId());
            if (request.getIsPrivate() != null) existing.setIsPrivate(request.getIsPrivate());
            existing.setScheduledAt(scheduledAt);
            baseMapper.updateById(existing);
            saveVersion(existing, getNextVersion(existing.getId()));
            tagService.updatePostTags(existing.getId(), request.getTags());
            linkPostImages(existing.getId(), request.getContent(), request.getCoverImage());
            log.info("草稿更新成功: id={}, slug={}", existing.getId(), existing.getSlug());
            return PostResponse.from(existing);
        }

        // 新建文章
        Post post = new Post();
        post.setSlug(request.getSlug());
        post.setTitle(request.getTitle());
        post.setDescription(request.getDescription());
        post.setContent(request.getContent());
        post.setTags(request.getTags());
        post.setStatus(newStatus);
        post.setAuthorName(request.getAuthorName());
        post.setAuthorId(userId);
        post.setViews(0);
        post.setLikes(0);
        post.setCoverImage(request.getCoverImage());
        post.setSeriesId(request.getSeriesId());
        post.setIsPrivate(request.getIsPrivate() != null ? request.getIsPrivate() : 0);
        post.setIsHidden(0);
        post.setScheduledAt(scheduledAt);
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

        // 改 slug 前先做全局唯一校验，避免撞 uk_slug 直接 500
        if (request.getSlug() != null && !request.getSlug().equals(post.getSlug())) {
            if (isSlugExists(request.getSlug(), userId, id)) {
                throw new BusinessException(400, "该 URL 标识已被占用，请更换");
            }
            post.setSlug(request.getSlug());
        }
        if (request.getTitle() != null) post.setTitle(request.getTitle());
        if (request.getDescription() != null) post.setDescription(request.getDescription());
        if (request.getContent() != null) post.setContent(request.getContent());
        if (request.getTags() != null) post.setTags(request.getTags());
        if (request.getStatus() != null) {
            int newStatus = normalizeStatus(request.getStatus());
            post.setStatus(newStatus);
            if (newStatus == 2) {
                java.time.LocalDateTime at = parseScheduledAt(newStatus, request.getScheduledAt());
                // 定时发布必须带时间，且已有时间时保留原值
                if (at != null) {
                    post.setScheduledAt(at);
                } else if (post.getScheduledAt() == null) {
                    throw new BusinessException(400, "定时发布需要指定发布时间");
                }
            } else {
                // 草稿/已发布不保留定时时间
                post.setScheduledAt(null);
            }
        }
        if (request.getAuthorName() != null) post.setAuthorName(request.getAuthorName());
        if (request.getCoverImage() != null) post.setCoverImage(request.getCoverImage());
        if (request.getSeriesId() != null) post.setSeriesId(request.getSeriesId());
        if (request.getIsPrivate() != null) post.setIsPrivate(request.getIsPrivate());
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
        // 清理关联数据：post_versions 与 images.post_id 之前不回收，
        // 会留下孤儿版本（含正文）和指向已删文章的图片记录。
        try {
            postVersionMapper.delete(new LambdaQueryWrapper<PostVersion>()
                    .eq(PostVersion::getPostId, id));
        } catch (Exception e) {
            log.warn("清理文章版本失败: postId={}, err={}", id, e.getMessage());
        }
        try {
            imageMapper.update(null, new LambdaUpdateWrapper<Image>()
                    .set(Image::getPostId, null)
                    .eq(Image::getPostId, id));
        } catch (Exception e) {
            log.warn("解除图片关联失败: postId={}, err={}", id, e.getMessage());
        }
        log.info("文章删除成功: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PostResponse getPostDetail(String slug, Long userId, String role) {
        Post post = getBySlug(slug);
        // 私有/隐藏校验收敛到这里，任何走详情的地方都受同一规则约束
        if (!com.blog.common.PostVisibility.canRead(post, userId, role)) {
            throw new ResourceNotFoundException("文章", "slug", slug);
        }
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
    public List<Post> getRelatedPosts(String slug, int limit, Long userId, String role) {
        Post current = getBySlug(slug);
        int safeLimit = Math.max(1, Math.min(limit, 100));
        if (current.getTags() == null || current.getTags().trim().isEmpty()) {
            LambdaQueryWrapper<Post> w = new LambdaQueryWrapper<Post>()
                    .eq(Post::getStatus, 1)
                    .ne(Post::getId, current.getId())
                    .orderByDesc(Post::getViews)
                    .last("LIMIT " + safeLimit);
            com.blog.common.PostVisibility.applyReadableFilter(w, userId, role);
            return baseMapper.selectList(w);
        }
        // 按标签匹配（通过 post_tag 关联表），拆分逗号分隔的标签
        List<Long> tagIds = new ArrayList<>();
        for (String tag : current.getTags().split(",")) {
            tagIds.addAll(tagService.getTagIdsBySlugOrName(tag.trim()));
        }
        tagIds = tagIds.stream().distinct().collect(Collectors.toList());
        if (tagIds.isEmpty()) {
            LambdaQueryWrapper<Post> w = new LambdaQueryWrapper<Post>()
                    .eq(Post::getStatus, 1)
                    .ne(Post::getId, current.getId())
                    .orderByDesc(Post::getViews)
                    .last("LIMIT " + safeLimit);
            com.blog.common.PostVisibility.applyReadableFilter(w, userId, role);
            return baseMapper.selectList(w);
        }
        LambdaQueryWrapper<Post> w = new LambdaQueryWrapper<Post>()
                .eq(Post::getStatus, 1)
                .ne(Post::getId, current.getId())
                .inSql(Post::getId,
                        "SELECT DISTINCT pt.post_id FROM post_tag pt WHERE pt.tag_id IN (" +
                        tagIds.stream().map(String::valueOf).collect(Collectors.joining(",")) + ")")
                .orderByDesc(Post::getViews)
                .last("LIMIT " + safeLimit);
        com.blog.common.PostVisibility.applyReadableFilter(w, userId, role);
        return baseMapper.selectList(w);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @XxlJob("publishScheduledPosts")
    public int publishScheduledPosts() {
        // 定时发布的文章状态为 2（与前端"定时发布"、updatePost 的写入口径一致）。
        // 之前这里查 status=0，而保存时 status 恒为 0 且 scheduled_at 被清空，任务永远匹配不到。
        int count = baseMapper.update(null, new LambdaUpdateWrapper<Post>()
                .set(Post::getStatus, 1)
                .set(Post::getScheduledAt, null)
                .eq(Post::getStatus, 2)
                .isNotNull(Post::getScheduledAt)
                .le(Post::getScheduledAt, LocalDateTime.now()));
        if (count > 0) {
            log.info("定时发布: {} 篇文章已发布", count);
        }
        XxlJobHelper.log("定时发布: {} 篇文章已发布", count);
        return count;
    }

    /**
     * 规范化文章状态：只允许 0(草稿) / 1(已发布) / 2(定时发布)。
     * 之前 status 没有任何范围校验，写 99 之类的值会让文章从所有列表里消失。
     */
    private int normalizeStatus(Integer status) {
        if (status == null) {
            return com.blog.common.enums.PostStatus.PUBLISHED.getCode();
        }
        if (status < 0 || status > 2) {
            throw new BusinessException(400, "文章状态非法，只能是 0=草稿 / 1=已发布 / 2=定时发布");
        }
        return status;
    }

    /**
     * 解析定时发布时间。只有 status=2（定时发布）才需要时间，其它状态一律返回 null。
     */
    private java.time.LocalDateTime parseScheduledAt(int status, String raw) {
        if (status != 2) {
            return null;
        }
        if (raw == null || raw.trim().isEmpty()) {
            throw new BusinessException(400, "定时发布需要指定发布时间");
        }
        try {
            return java.time.LocalDateTime.parse(raw.trim());
        } catch (Exception e) {
            throw new BusinessException(400, "定时发布时间格式不正确，应为 yyyy-MM-ddTHH:mm:ss");
        }
    }

    @Override
    public List<Post> getRankingByLikes(int limit, String role) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        boolean isAdmin = "admin".equals(role);
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<Post>()
                .eq(Post::getStatus, 1)
                .eq(Post::getIsPrivate, 0);
        if (!isAdmin) {
            wrapper.eq(Post::getIsHidden, 0);
        }
        wrapper.orderByDesc(Post::getLikes)
               .last("LIMIT " + safeLimit);
        return baseMapper.selectList(wrapper);
    }

    @Override
    public IPage<Post> searchPosts(String query, int page, int size, String role, Long userId) {
        boolean isAdmin = "admin".equals(role);
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<Post>()
                .eq(Post::getStatus, 1);
        // 管理员可以看到隐藏文章
        if (!isAdmin) {
            wrapper.eq(Post::getIsHidden, 0);
        }
        // 私有文章仅作者本人可见
        if (userId != null) {
            wrapper.apply("(is_private = 0 OR author_id = {0})", userId);
        } else {
            wrapper.eq(Post::getIsPrivate, 0);
        }
        if (query != null && !query.trim().isEmpty()) {
            String q = query.trim();
            wrapper.apply("(title LIKE {0} OR content LIKE {1} OR description LIKE {2} OR tags LIKE {3})",
                    "%" + q + "%", "%" + q + "%", "%" + q + "%", "%" + q + "%");
        }
        wrapper.orderByDesc(Post::getLikes);
        return baseMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public List<Map<String, Object>> getArchive() {
        List<Post> posts = baseMapper.selectList(
                new LambdaQueryWrapper<Post>()
                        .eq(Post::getStatus, 1)
                        .eq(Post::getIsPrivate, 0)
                        .eq(Post::getIsHidden, 0)
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
