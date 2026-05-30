package com.blog.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评论点赞/点踩实体
 */
@Data
@TableName("comment_reaction")
public class CommentReaction {

    private Long id;
    private Long commentId;
    private Long userId;
    private String ip;
    private Integer reactionType; // 1=赞, 0=踩
    private LocalDateTime createdAt;
}
