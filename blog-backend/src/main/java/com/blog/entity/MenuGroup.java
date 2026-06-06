package com.blog.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.blog.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 菜单分组
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("menu_group")
public class MenuGroup extends BaseEntity {

    private String name;

    private String icon;

    private Integer sortOrder;

    private Integer enabled;

    private Integer adminOnly;
}
