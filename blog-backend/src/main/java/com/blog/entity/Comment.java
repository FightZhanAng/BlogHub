package com.blog.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.blog.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 评论实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("comments")
public class Comment extends BaseEntity {

    /** 关联文章 ID */
    private Long postId;

    /** 父评论 ID（回复用） */
    private Long parentId;

    /** 评论者昵称 */
    private String nickname;

    /** 评论者用户 ID（未登录可为 null） */
    private Long userId;

    /** 评论内容 */
    private String content;

    /** 点赞数 */
    private Integer likes;

    /** 踩数 */
    private Integer dislikes;
}
