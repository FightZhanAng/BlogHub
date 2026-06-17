package com.blog.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.common.PageResult;
import com.blog.common.Result;
import com.blog.config.JwtUtil;
import com.blog.entity.SysLog;
import com.blog.mapper.SysLogMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
@Tag(name = "鎿嶄綔鏃ュ織", description = "绯荤粺鎿嶄綔鏃ュ織鏌ヨ")
public class LogController {

    private final SysLogMapper sysLogMapper;

    private final JwtUtil jwtUtil;

    private final HttpServletRequest request;

    private boolean isAdmin() {
        try {
            String header = request.getHeader("Authorization");
            if (header != null && header.startsWith("Bearer ")) {
                String token = header.substring(7);
                return "admin".equals(jwtUtil.getRole(token));
            }
        } catch (Exception e) { log.debug("操作失败: {}", e.getMessage()); }
        return false;
    }

    @Operation(summary = "鍒嗛〉鏌ヨ鎿嶄綔鏃ュ織")
    @GetMapping
    public Result<PageResult<SysLog>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (!isAdmin()) return Result.error(403, "鏃犳潈璁块棶");

        IPage<SysLog> ipage = sysLogMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<SysLog>()
                        .orderByDesc(SysLog::getCreatedAt));
        return Result.success(PageResult.of(ipage));
    }
}
