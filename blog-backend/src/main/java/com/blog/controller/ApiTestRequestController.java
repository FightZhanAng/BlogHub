package com.blog.controller;

import com.blog.common.Result;
import com.blog.dto.ApiTestRequestDTO;
import com.blog.entity.ApiTestRequest;
import com.blog.service.ApiTestRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/tester")
@RequiredArgsConstructor
@Tag(name = "API 测试工具", description = "请求管理")
public class ApiTestRequestController {

    private final ApiTestRequestService requestService;

    @Operation(summary = "获取集合下的请求列表")
    @GetMapping("/collections/{collectionId}/requests")
    public Result<List<ApiTestRequest>> listByCollection(@PathVariable Long collectionId) {
        return Result.success(requestService.listByCollection(collectionId));
    }

    @Operation(summary = "保存请求")
    @PostMapping("/requests")
    public Result<ApiTestRequest> save(@Valid @RequestBody ApiTestRequestDTO dto,
                                       HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(requestService.save(dto, userId));
    }

    @Operation(summary = "更新请求")
    @PutMapping("/requests/{id}")
    public Result<ApiTestRequest> update(@PathVariable Long id,
                                         @Valid @RequestBody ApiTestRequestDTO dto,
                                         HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(requestService.update(id, dto, userId));
    }

    @Operation(summary = "删除请求")
    @DeleteMapping("/requests/{id}")
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        requestService.delete(id, userId);
        return Result.success(null);
    }
}
