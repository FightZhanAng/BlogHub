package com.blog.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.common.Result;
import com.blog.entity.ApiTestHistory;
import com.blog.service.ApiTestHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/tester/history")
@RequiredArgsConstructor
@Tag(name = "API 测试工具", description = "请求历史管理")
public class ApiTestHistoryController {

    private final ApiTestHistoryService historyService;

    @Operation(summary = "获取历史记录列表")
    @GetMapping
    public Result<Page<ApiTestHistory>> list(@RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "20") int size,
                                             HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(historyService.listByUser(userId, page, size));
    }

    @Operation(summary = "删除单条历史记录")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        historyService.delete(id, userId);
        return Result.success(null);
    }

    @Operation(summary = "清空历史记录")
    @DeleteMapping
    public Result<Void> clear(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        historyService.clear(userId);
        return Result.success(null);
    }
}
