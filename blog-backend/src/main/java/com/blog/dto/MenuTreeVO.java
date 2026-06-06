package com.blog.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 菜单树 VO（前端侧边栏用）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MenuTreeVO {

    /** 菜单项 ID */
    private Long id;

    /** 菜单标题 */
    private String title;

    /** 路由路径（顶级分组为 null） */
    private String path;

    /** Element Plus 图标名 */
    private String icon;

    /** 分组标识（用于 el-sub-menu index），独立顶级项为 null */
    private String groupKey;

    /** 是否仅管理员可见 */
    private Boolean adminOnly;

    /** 子菜单项（分组才有） */
    private List<MenuTreeVO> children;
}
