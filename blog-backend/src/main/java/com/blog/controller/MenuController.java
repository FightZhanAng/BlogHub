package com.blog.controller;

import com.blog.common.Result;
import com.blog.dto.MenuSortRequest;
import com.blog.dto.MenuTreeVO;
import com.blog.dto.MenuUnifiedSortRequest;
import com.blog.entity.MenuGroup;
import com.blog.entity.MenuItem;
import com.blog.service.MenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/menus")
@RequiredArgsConstructor
@Tag(name = "菜单管理", description = "动态菜单配置")
public class MenuController {

    private final MenuService menuService;

    // ========== 前端侧边栏用 ==========

    @Operation(summary = "获取菜单树（根据当前用户角色过滤）")
    @GetMapping("/tree")
    public Result<List<MenuTreeVO>> getTree(HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        return Result.success(menuService.getMenuTree(role));
    }

    // ========== 管理页用：菜单组 CRUD ==========

    @Operation(summary = "获取所有菜单组（含禁用项）")
    @GetMapping("/groups")
    public Result<List<MenuGroup>> listGroups() {
        return Result.success(menuService.listGroups());
    }

    @Operation(summary = "创建菜单组")
    @PostMapping("/groups")
    public Result<MenuGroup> createGroup(@RequestBody MenuGroup group) {
        return Result.created(menuService.createGroup(group));
    }

    @Operation(summary = "更新菜单组")
    @PutMapping("/groups/{id}")
    public Result<MenuGroup> updateGroup(@PathVariable Long id, @RequestBody MenuGroup group) {
        return Result.success(menuService.updateGroup(id, group));
    }

    @Operation(summary = "删除菜单组")
    @DeleteMapping("/groups/{id}")
    public Result<Void> deleteGroup(@PathVariable Long id) {
        menuService.deleteGroup(id);
        return Result.noContent();
    }

    @Operation(summary = "菜单组排序")
    @PutMapping("/groups/sort")
    public Result<Void> sortGroups(@Valid @RequestBody MenuSortRequest req) {
        menuService.updateGroupSort(req.getIds());
        return Result.success(null);
    }

    // ========== 管理页用：菜单项 CRUD ==========

    @Operation(summary = "获取所有菜单项")
    @GetMapping("/items")
    public Result<List<MenuItem>> listItems(@RequestParam(required = false) Long groupId) {
        if (groupId != null) {
            return Result.success(menuService.listItems(groupId));
        }
        return Result.success(menuService.listAllItems());
    }

    @Operation(summary = "创建菜单项")
    @PostMapping("/items")
    public Result<MenuItem> createItem(@RequestBody MenuItem item) {
        return Result.created(menuService.createItem(item));
    }

    @Operation(summary = "更新菜单项")
    @PutMapping("/items/{id}")
    public Result<MenuItem> updateItem(@PathVariable Long id, @RequestBody MenuItem item) {
        return Result.success(menuService.updateItem(id, item));
    }

    @Operation(summary = "删除菜单项")
    @DeleteMapping("/items/{id}")
    public Result<Void> deleteItem(@PathVariable Long id) {
        menuService.deleteItem(id);
        return Result.noContent();
    }

    @Operation(summary = "菜单项排序")
    @PutMapping("/items/sort")
    public Result<Void> sortItems(@Valid @RequestBody MenuSortRequest req) {
        menuService.updateItemSort(req.getIds());
        return Result.success(null);
    }

    @Operation(summary = "统一排序（顶级项 + 菜单组混合排序）")
    @PutMapping("/sort-all")
    public Result<Void> sortAll(@Valid @RequestBody MenuUnifiedSortRequest req) {
        menuService.sortAll(req.getEntries());
        return Result.success(null);
    }
}
