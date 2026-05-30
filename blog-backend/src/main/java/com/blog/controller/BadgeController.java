package com.blog.controller;

import com.blog.common.Result;
import com.blog.entity.Badge;
import com.blog.service.BadgeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "徽章管理", description = "用户徽章和成就系统")
public class BadgeController {

    private final BadgeService badgeService;

    @Operation(summary = "获取所有徽章定义")
    @GetMapping("/badges")
    public Result<List<Badge>> getAllBadges() {
        return Result.success(badgeService.getAllBadges());
    }

    @Operation(summary = "获取用户已获得的徽章")
    @GetMapping("/users/{id}/badges")
    public Result<List<Map<String, Object>>> getUserBadges(@PathVariable Long id) {
        return Result.success(badgeService.getUserBadges(id));
    }

    @Operation(summary = "标记徽章通知已读")
    @PutMapping("/users/{id}/badges/mark-read")
    public Result<Void> markBadgesRead(@PathVariable Long id) {
        badgeService.markBadgesRead(id);
        return Result.success(null);
    }
}
