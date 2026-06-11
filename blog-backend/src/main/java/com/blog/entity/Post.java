package com.blog.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.blog.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文章实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("posts")
public class Post extends BaseEntity {

    /** URL 友好标识 */
    private String slug;

    /** 文章标题 */
    private String title;

    /** 文章摘要 */
    private String description;

    /** Markdown 正文 */
    private String content;

    /** 标签（逗号分隔） */
    private String tags;

    /** 状态: 0=草稿, 1=已发布 */
    private Integer status;

    /** 阅读次数 */
    private Integer views;

    /** 点赞数 */
    private Integer likes;

    /** 作者名 */
    private String authorName;

    /** 作者用户 ID */
    private Long authorId;

    /** 封面图 URL */
    private String coverImage;

    /** 定时发布时间 */
    private java.time.LocalDateTime scheduledAt;

    /** 所属系列 ID */
    private Long seriesId;

    /** 是否置顶 0=否 1=是 */
    private Integer isPinned;

    private Integer isPrivate;

    private Integer isHidden;
}
