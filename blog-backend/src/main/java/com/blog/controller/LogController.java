package com.blog.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.common.PageResult;
import com.blog.common.Result;
import com.blog.config.JwtUtil;
import com.blog.entity.SysLog;
import com.blog.mapper.SysLogMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/logs")
public class LogController {

    @Autowired
    private SysLogMapper sysLogMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private HttpServletRequest request;

    private boolean isAdmin() {
        try {
            String header = request.getHeader("Authorization");
            if (header != null && header.startsWith("Bearer ")) {
                String token = header.substring(7);
                return "admin".equals(jwtUtil.getRole(token));
            }
        } catch (Exception ignored) {}
        return false;
    }

    @GetMapping
    public Result<PageResult<SysLog>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (!isAdmin()) return Result.error(403, "无权访问");

        IPage<SysLog> ipage = sysLogMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<SysLog>()
                        .orderByDesc(SysLog::getCreatedAt));
        return Result.success(PageResult.of(ipage));
    }
}
