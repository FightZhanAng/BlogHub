package com.blog.service;

import com.blog.dto.MenuTreeVO;
import com.blog.dto.MenuUnifiedSortRequest;
import com.blog.entity.MenuGroup;
import com.blog.entity.MenuItem;

import java.util.List;

public interface MenuService {

    /** 获取菜单树（根据角色过滤） */
    List<MenuTreeVO> getMenuTree(String role);

    /** 获取所有菜单组（管理页用，含禁用项） */
    List<MenuGroup> listGroups();

    /** 创建菜单组 */
    MenuGroup createGroup(MenuGroup group);

    /** 更新菜单组 */
    MenuGroup updateGroup(Long id, MenuGroup group);

    /** 删除菜单组（同时删除组下菜单项） */
    void deleteGroup(Long id);

    /** 更新菜单组排序 */
    void updateGroupSort(List<Long> ids);

    /** 获取指定组的菜单项（管理页用，含禁用项） */
    List<MenuItem> listItems(Long groupId);

    /** 获取所有菜单项 */
    List<MenuItem> listAllItems();

    /** 创建菜单项 */
    MenuItem createItem(MenuItem item);

    /** 更新菜单项 */
    MenuItem updateItem(Long id, MenuItem item);

    /** 删除菜单项 */
    void deleteItem(Long id);

    /** 更新菜单项排序 */
    void updateItemSort(List<Long> ids);

    /** 统一排序（顶级项 + 菜单组混合排序） */
    void sortAll(List<MenuUnifiedSortRequest.SortEntry> entries);
}
