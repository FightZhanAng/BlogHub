package com.blog.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.blog.entity.Post;
import com.blog.entity.Tag;

import java.util.List;

public interface TagService extends IService<Tag> {

    /** 获取所有标签（按文章数降序） */
    List<Tag> getAllTags();

    /** 按 slug 查询标签 */
    Tag getBySlug(String slug);

    /** 获取标签下的文章（分页） */
    IPage<Post> getTagPosts(String slug, int page, int size);

    /** 查找或创建标签，返回 tagId */
    Long findOrCreate(String name);

    /** 更新文章 tags：删除旧关联，建立新关联 */
    void updatePostTags(Long postId, String tagsStr);
}
