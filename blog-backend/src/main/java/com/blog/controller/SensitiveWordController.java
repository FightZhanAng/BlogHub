package com.blog.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.blog.common.Result;
import com.blog.entity.SensitiveWord;
import com.blog.service.SensitiveWordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/sensitive-words")
@RequiredArgsConstructor
@Tag(name = "敏感词管理", description = "管理员敏感词库管理")
public class SensitiveWordController {

    private final SensitiveWordService sensitiveWordService;

    @Operation(summary = "分页查询敏感词")
    @GetMapping
    public Result<IPage<SensitiveWord>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword) {
        return Result.success(sensitiveWordService.list(page, size, keyword));
    }

    @Operation(summary = "添加敏感词")
    @PostMapping
    public Result<SensitiveWord> add(@RequestBody SensitiveWord word) {
        return Result.success(sensitiveWordService.add(word));
    }

    @Operation(summary = "修改敏感词")
    @PutMapping("/{id}")
    public Result<SensitiveWord> update(@PathVariable Long id, @RequestBody SensitiveWord word) {
        return Result.success(sensitiveWordService.update(id, word));
    }

    @Operation(summary = "删除敏感词")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        sensitiveWordService.delete(id);
        return Result.success();
    }

    @Operation(summary = "批量导入敏感词")
    @PostMapping("/batch")
    public Result<Map<String, Integer>> batchAdd(@RequestBody List<SensitiveWord> words) {
        int count = sensitiveWordService.batchAdd(words);
        Map<String, Integer> result = new java.util.HashMap<>();
        result.put("imported", count);
        result.put("total", words.size());
        return Result.success(result);
    }

    @Operation(summary = "热加载敏感词库")
    @PostMapping("/reload")
    public Result<Void> reload() {
        sensitiveWordService.reload();
        return Result.success();
    }
}
