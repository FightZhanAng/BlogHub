package com.blog.controller;

import com.blog.common.Result;
import com.blog.service.HotTopicService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


@Tag(name = "每日热点")
@RestController
@RequestMapping("/api/hot-topics")
public class HotTopicController {

    @Autowired
    private HotTopicService hotTopicService;

    @Operation(summary = "获取今日热点（按平台分组）")
    @GetMapping
    public Result<?> getTodayTopics() {
        return Result.success(hotTopicService.getTodayTopics());
    }

    @Operation(summary = "获取指定平台热点")
    @GetMapping("/{platform}")
    public Result<?> getByPlatform(@PathVariable String platform) {
        return Result.success(hotTopicService.getTopicsByPlatform(platform));
    }

    @Operation(summary = "手动触发抓取（管理员）")
    @PostMapping("/refresh")
    public Result<?> refresh() {
        hotTopicService.fetchAllTopics();
        return Result.success("热点数据已刷新");
    }
}
