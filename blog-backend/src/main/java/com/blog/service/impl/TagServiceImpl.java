package com.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.blog.entity.Post;
import com.blog.entity.PostTag;
import com.blog.entity.Tag;
import com.blog.mapper.PostMapper;
import com.blog.mapper.PostTagMapper;
import com.blog.mapper.TagMapper;
import com.blog.service.TagService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag> implements TagService {

    private static final Logger log = LoggerFactory.getLogger(TagServiceImpl.class);

    private final TagMapper tagMapper;

    private final PostTagMapper postTagMapper;

    private final PostMapper postMapper;

    @Override
    @Cacheable(value = "tags", key = "'all'")
    public List<Tag> getAllTags() {
        return tagMapper.selectList(
                new LambdaQueryWrapper<Tag>()
                        .orderByDesc(Tag::getPostCount)
        );
    }

    @Override
    public Tag getBySlug(String slug) {
        return tagMapper.selectOne(
                new LambdaQueryWrapper<Tag>()
                        .eq(Tag::getSlug, slug)
        );
    }

    @Override
    public IPage<Post> getTagPosts(String slug, int page, int size) {
        Tag tag = getBySlug(slug);
        if (tag == null) {
            return new Page<>(page, size, 0);
        }
        // 查询关联该标签的已发布文章（按创建时间降序）
        return postMapper.selectTagPosts(new Page<>(page, size), tag.getId());
    }

    @Override
    @Transactional
    @CacheEvict(value = "tags", allEntries = true)
    public Long findOrCreate(String name) {
        if (name == null || name.trim().isEmpty()) return null;
        String tagName = name.trim();

        // 查找已有标签
        Tag existing = tagMapper.selectOne(
                new LambdaQueryWrapper<Tag>().eq(Tag::getName, tagName)
        );
        if (existing != null) {
            return existing.getId();
        }

        // 创建新标签
        Tag tag = new Tag();
        tag.setName(tagName);
        tag.setSlug(generateSlug(tagName));
        tag.setPostCount(0);
        tagMapper.insert(tag);
        log.info("Created new tag: {} (slug: {})", tagName, tag.getSlug());
        return tag.getId();
    }

    @Override
    @Transactional
    @CacheEvict(value = "tags", key = "'all'")
    public void updatePostTags(Long postId, String tagsStr) {
        // 删除旧关联
        postTagMapper.delete(
                new LambdaQueryWrapper<PostTag>().eq(PostTag::getPostId, postId)
        );

        if (tagsStr == null || tagsStr.trim().isEmpty()) return;

        // 解析标签名
        String[] names = tagsStr.split(",");
        for (String name : names) {
            Long tagId = findOrCreate(name);
            if (tagId == null) continue;

            PostTag pt = new PostTag();
            pt.setPostId(postId);
            pt.setTagId(tagId);
            postTagMapper.insert(pt);
        }

        // 更新所有 tag 的 post_count
        recalcAllPostCounts();
    }

    /** 重新计算所有标签的文章数（排除隐藏文章） */
    @Override
    public void recalcAllPostCounts() {
        List<Tag> allTags = tagMapper.selectList(null);
        for (Tag tag : allTags) {
            Long count = postTagMapper.selectCount(
                    new LambdaQueryWrapper<PostTag>().eq(PostTag::getTagId, tag.getId())
                        .inSql(PostTag::getPostId,
                            "SELECT id FROM posts WHERE status = 1 AND is_hidden = 0")
            );
            tag.setPostCount(count.intValue());
            tagMapper.updateById(tag);
        }
    }

    @Override
    public List<Long> getTagIdsBySlugOrName(String tag) {
        return tagMapper.selectList(
                new LambdaQueryWrapper<Tag>()
                        .eq(Tag::getSlug, tag)
                        .or()
                        .eq(Tag::getName, tag))
                .stream()
                .map(Tag::getId)
                .collect(Collectors.toList());
    }

    /** 从标签名生成 slug */
    private String generateSlug(String name) {
        String slug = name.toLowerCase()
                .trim()
                .replaceAll("[^a-z0-9\\u4e00-\\u9fff]+", "-")
                .replaceAll("^-|-$", "");
        // 同名 slug 去重
        String finalSlug = slug;
        long count = tagMapper.selectCount(
                new LambdaQueryWrapper<Tag>().eq(Tag::getSlug, finalSlug)
        );
        if (count > 0) {
            return slug + "-" + System.currentTimeMillis();
        }
        return slug;
    }
}
