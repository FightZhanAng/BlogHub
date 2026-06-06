package com.blog.controller;

import com.blog.common.Result;
import com.blog.dto.ApiTestEnvDTO;
import com.blog.entity.ApiTestEnv;
import com.blog.service.ApiTestEnvService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/tester/env")
@RequiredArgsConstructor
@Tag(name = "API 测试工具", description = "环境变量管理")
public class ApiTestEnvController {

    private final ApiTestEnvService envService;

    @Operation(summary = "获取用户的环境变量列表")
    @GetMapping
    public Result<List<ApiTestEnv>> list(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(envService.listByUser(userId));
    }

    @Operation(summary = "创建环境变量")
    @PostMapping
    public Result<ApiTestEnv> create(@Valid @RequestBody ApiTestEnvDTO dto,
                                     HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(envService.create(dto, userId));
    }

    @Operation(summary = "更新环境变量")
    @PutMapping("/{id}")
    public Result<ApiTestEnv> update(@PathVariable Long id,
                                     @Valid @RequestBody ApiTestEnvDTO dto,
                                     HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(envService.update(id, dto, userId));
    }

    @Operation(summary = "删除环境变量")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        envService.delete(id, userId);
        return Result.success(null);
    }
}
