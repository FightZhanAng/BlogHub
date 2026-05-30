package com.blog.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.blog.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 徽章定义实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("badge")
public class Badge extends BaseEntity {

    /** 徽章名称 */
    private String name;

    /** 图标 */
    private String icon;

    /** 描述 */
    private String description;

    /** 分类: interaction/content/activity/special */
    private String category;

    /** 排序 */
    private Integer sortOrder;

    /** 是否启用 */
    private Integer isActive;
}
