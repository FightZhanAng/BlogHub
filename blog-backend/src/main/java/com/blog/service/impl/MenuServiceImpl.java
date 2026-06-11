package com.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.dto.MenuTreeVO;
import com.blog.dto.MenuUnifiedSortRequest;
import com.blog.entity.MenuGroup;
import com.blog.entity.MenuItem;
import com.blog.exception.BusinessException;
import com.blog.mapper.MenuGroupMapper;
import com.blog.mapper.MenuItemMapper;
import com.blog.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MenuServiceImpl implements MenuService {

    private final MenuGroupMapper groupMapper;
    private final MenuItemMapper itemMapper;

    @Override
    public List<MenuTreeVO> getMenuTree(String role) {
        boolean isAdmin = "admin".equals(role);

        // 查所有启用的分组，按 sort_order 排序
        List<MenuGroup> groups = groupMapper.selectList(
            new LambdaQueryWrapper<MenuGroup>()
                .eq(MenuGroup::getEnabled, 1)
                .orderByAsc(MenuGroup::getSortOrder)
        );

        // 查所有启用的菜单项，按 sort_order 排序
        List<MenuItem> items = itemMapper.selectList(
            new LambdaQueryWrapper<MenuItem>()
                .eq(MenuItem::getEnabled, 1)
                .orderByAsc(MenuItem::getSortOrder)
        );

        // 按 groupId 分组
        Map<Long, List<MenuItem>> groupedItems = items.stream()
            .filter(i -> i.getGroupId() != null)
            .collect(Collectors.groupingBy(MenuItem::getGroupId));

        List<MenuTreeVO> tree = new ArrayList<>();

        // 合并独立顶级项和分组，按 sort_order 统一排序
        // 先收集顶级项和分组，放入统一列表
        List<Object[]> unified = new ArrayList<>();
        items.stream()
            .filter(i -> i.getGroupId() == null)
            .forEach(i -> unified.add(new Object[]{"item", i, i.getSortOrder()}));
        for (MenuGroup g : groups) {
            unified.add(new Object[]{"group", g, g.getSortOrder()});
        }
        unified.sort(Comparator.comparingInt(o -> (int) o[2]));

        for (Object[] entry : unified) {
            String type = (String) entry[0];

            if ("item".equals(type)) {
                MenuItem i = (MenuItem) entry[1];
                if (isAdmin || i.getAdminOnly() == 0) {
                    tree.add(new MenuTreeVO(
                        i.getId(), i.getTitle(), i.getPath(), i.getIcon(),
                        null, i.getAdminOnly() == 1, null
                    ));
                }
            } else {
                MenuGroup group = (MenuGroup) entry[1];
                if (!isAdmin && group.getAdminOnly() == 1) continue;

            List<MenuItem> groupItems = groupedItems.getOrDefault(group.getId(), Collections.emptyList());
            List<MenuTreeVO> children = new ArrayList<>();

            for (MenuItem item : groupItems) {
                if (!isAdmin && item.getAdminOnly() == 1) continue;
                children.add(new MenuTreeVO(
                    item.getId(), item.getTitle(), item.getPath(), item.getIcon(),
                    null, item.getAdminOnly() == 1, null
                ));
            }

            if (!children.isEmpty()) {
                tree.add(new MenuTreeVO(
                    group.getId(), group.getName(), null, group.getIcon(),
                    group.getId() + "-group", group.getAdminOnly() == 1, children
                ));
            }
            }
        }

        return tree;
    }

    // ========== 菜单组 CRUD ==========

    @Override
    public List<MenuGroup> listGroups() {
        return groupMapper.selectList(
            new LambdaQueryWrapper<MenuGroup>().orderByAsc(MenuGroup::getSortOrder)
        );
    }

    @Override
    public MenuGroup createGroup(MenuGroup group) {
        if (group.getSortOrder() == null) {
            // 默认排到最后
            Long max = groupMapper.selectCount(null);
            group.setSortOrder(max.intValue() + 1);
        }
        if (group.getEnabled() == null) group.setEnabled(1);
        if (group.getAdminOnly() == null) group.setAdminOnly(0);
        groupMapper.insert(group);
        return group;
    }

    @Override
    public MenuGroup updateGroup(Long id, MenuGroup group) {
        MenuGroup existing = groupMapper.selectById(id);
        if (existing == null) throw new BusinessException(404, "菜单组不存在");

        existing.setName(group.getName());
        existing.setIcon(group.getIcon());
        if (group.getSortOrder() != null) existing.setSortOrder(group.getSortOrder());
        if (group.getEnabled() != null) existing.setEnabled(group.getEnabled());
        if (group.getAdminOnly() != null) existing.setAdminOnly(group.getAdminOnly());
        groupMapper.updateById(existing);
        return existing;
    }

    @Override
    @Transactional
    public void deleteGroup(Long id) {
        // 先删除组下所有菜单项
        itemMapper.delete(
            new LambdaQueryWrapper<MenuItem>().eq(MenuItem::getGroupId, id)
        );
        groupMapper.deleteById(id);
    }

    @Override
    public void updateGroupSort(List<Long> ids) {
        for (int i = 0; i < ids.size(); i++) {
            MenuGroup group = groupMapper.selectById(ids.get(i));
            if (group != null) {
                group.setSortOrder(i + 1);
                groupMapper.updateById(group);
            }
        }
    }

    // ========== 菜单项 CRUD ==========

    @Override
    public List<MenuItem> listItems(Long groupId) {
        return itemMapper.selectList(
            new LambdaQueryWrapper<MenuItem>()
                .eq(groupId != null, MenuItem::getGroupId, groupId)
                .isNull(groupId == null, MenuItem::getGroupId)
                .orderByAsc(MenuItem::getSortOrder)
        );
    }

    @Override
    public List<MenuItem> listAllItems() {
        return itemMapper.selectList(
            new LambdaQueryWrapper<MenuItem>().orderByAsc(MenuItem::getSortOrder)
        );
    }

    @Override
    public MenuItem createItem(MenuItem item) {
        if (item.getSortOrder() == null) {
            Long count = itemMapper.selectCount(
                new LambdaQueryWrapper<MenuItem>()
                    .eq(item.getGroupId() != null, MenuItem::getGroupId, item.getGroupId())
                    .isNull(item.getGroupId() == null, MenuItem::getGroupId)
            );
            item.setSortOrder(count.intValue() + 1);
        }
        if (item.getEnabled() == null) item.setEnabled(1);
        if (item.getAdminOnly() == null) item.setAdminOnly(0);
        itemMapper.insert(item);
        return item;
    }

    @Override
    public MenuItem updateItem(Long id, MenuItem item) {
        MenuItem existing = itemMapper.selectById(id);
        if (existing == null) throw new BusinessException(404, "菜单项不存在");

        existing.setTitle(item.getTitle());
        existing.setPath(item.getPath());
        existing.setIcon(item.getIcon());
        if (item.getGroupId() != null) existing.setGroupId(item.getGroupId());
        if (item.getSortOrder() != null) existing.setSortOrder(item.getSortOrder());
        if (item.getEnabled() != null) existing.setEnabled(item.getEnabled());
        if (item.getAdminOnly() != null) existing.setAdminOnly(item.getAdminOnly());
        itemMapper.updateById(existing);
        return existing;
    }

    @Override
    public void deleteItem(Long id) {
        itemMapper.deleteById(id);
    }

    @Override
    public void updateItemSort(List<Long> ids) {
        for (int i = 0; i < ids.size(); i++) {
            MenuItem item = itemMapper.selectById(ids.get(i));
            if (item != null) {
                item.setSortOrder(i + 1);
                itemMapper.updateById(item);
            }
        }
    }

    @Override
    public void sortAll(List<MenuUnifiedSortRequest.SortEntry> entries) {
        for (int i = 0; i < entries.size(); i++) {
            MenuUnifiedSortRequest.SortEntry entry = entries.get(i);
            int order = i + 1;
            if ("group".equals(entry.getType())) {
                MenuGroup group = groupMapper.selectById(entry.getId());
                if (group != null) {
                    group.setSortOrder(order);
                    groupMapper.updateById(group);
                }
            } else if ("item".equals(entry.getType())) {
                MenuItem item = itemMapper.selectById(entry.getId());
                if (item != null) {
                    item.setSortOrder(order);
                    itemMapper.updateById(item);
                }
            }
        }
    }
}
