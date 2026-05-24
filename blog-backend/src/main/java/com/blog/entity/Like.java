package com.blog.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.blog.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 点赞实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("likes")
public class Like extends BaseEntity {

    /** 关联文章 ID */
    private Long postId;

    /** 访客标识 */
    private String visitorId;

    /** 1=已赞, 0=取消 */
    private Integer liked;
}
