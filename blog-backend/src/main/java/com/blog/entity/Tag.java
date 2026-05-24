package com.blog.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.blog.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 标签实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tag")
public class Tag extends BaseEntity {

    /** 标签显示名 */
    private String name;

    /** URL 友好标识 */
    private String slug;

    /** 关联文章数（冗余字段） */
    private Integer postCount;
}
