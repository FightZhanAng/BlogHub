package com.blog.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.blog.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 菜单项
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("menu_item")
public class MenuItem extends BaseEntity {

    private Long groupId;

    private String title;

    private String path;

    private String icon;

    private Integer sortOrder;

    private Integer enabled;

    private Integer adminOnly;
}
