package com.blog.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.blog.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 收藏实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bookmarks")
public class Bookmark extends BaseEntity {

    /** 关联文章 ID */
    private Long postId;

    /** 访客标识 */
    private String visitorId;
}
