package com.blog.controller;

import com.blog.common.Result;
import com.blog.dto.ApiTestCollectionDTO;
import com.blog.entity.ApiTestCollection;
import com.blog.service.ApiTestCollectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/tester/collections")
@RequiredArgsConstructor
@Tag(name = "API 测试工具", description = "请求集合管理")
public class ApiTestCollectionController {

    private final ApiTestCollectionService collectionService;

    @Operation(summary = "获取用户的集合列表")
    @GetMapping
    public Result<List<ApiTestCollection>> list(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(collectionService.listByUser(userId));
    }

    @Operation(summary = "创建集合")
    @PostMapping
    public Result<ApiTestCollection> create(@Valid @RequestBody ApiTestCollectionDTO dto,
                                            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(collectionService.create(dto, userId));
    }

    @Operation(summary = "更新集合")
    @PutMapping("/{id}")
    public Result<ApiTestCollection> update(@PathVariable Long id,
                                            @Valid @RequestBody ApiTestCollectionDTO dto,
                                            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(collectionService.update(id, dto, userId));
    }

    @Operation(summary = "删除集合")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        collectionService.delete(id, userId);
        return Result.success(null);
    }
}
