package com.blog.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.entity.Comment;
import com.blog.entity.Post;
import com.blog.entity.User;
import com.blog.mapper.CommentMapper;
import com.blog.mapper.PostMapper;
import com.blog.mapper.UserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 数据导出接口（仅管理员，CSV 格式）
 */
@RestController
@RequestMapping("/api/export")
@RequiredArgsConstructor
@Tag(name = "数据导出", description = "CSV格式数据导出")
public class ExportController {

    private final PostMapper postMapper;

    private final UserMapper userMapper;

    private final CommentMapper commentMapper;

    private final com.blog.config.JwtUtil jwtUtil;

    private boolean isAdmin(HttpServletRequest req) {
        try {
            String h = req.getHeader("Authorization");
            if (h != null && h.startsWith("Bearer ")) {
                return "admin".equals(jwtUtil.getRole(h.substring(7)));
            }
        } catch (Exception ignored) {}
        return false;
    }

    @Operation(summary = "导出文章数据")
    @GetMapping("/posts")
    public ResponseEntity<byte[]> exportPosts(HttpServletRequest req) {
        if (!isAdmin(req)) return ResponseEntity.status(403).build();
        List<Post> list = postMapper.selectList(null);
        String csv = "ID,标题,Slug,阅读量,点赞,状态,创建时间\n" + list.stream()
                .map(p -> String.join(",", String.valueOf(p.getId()), escape(p.getTitle()), p.getSlug(),
                        String.valueOf(p.getViews() != null ? p.getViews() : 0),
                        String.valueOf(p.getLikes() != null ? p.getLikes() : 0),
                        String.valueOf(p.getStatus()),
                        p.getCreatedAt() != null ? p.getCreatedAt().toString() : ""))
                .collect(Collectors.joining("\n"));
        return csvResponse("posts_" + LocalDate.now() + ".csv", csv);
    }

    @Operation(summary = "导出用户数据")
    @GetMapping("/users")
    public ResponseEntity<byte[]> exportUsers(HttpServletRequest req) {
        if (!isAdmin(req)) return ResponseEntity.status(403).build();
        List<User> list = userMapper.selectList(null);
        String csv = "ID,用户名,昵称,角色,状态,创建时间\n" + list.stream()
                .map(u -> String.join(",", String.valueOf(u.getId()), u.getUsername(),
                        u.getNickname() != null ? escape(u.getNickname()) : "",
                        u.getRole(), String.valueOf(u.getStatus()),
                        u.getCreatedAt() != null ? u.getCreatedAt().toString() : ""))
                .collect(Collectors.joining("\n"));
        return csvResponse("users_" + LocalDate.now() + ".csv", csv);
    }

    @Operation(summary = "导出评论数据")
    @GetMapping("/comments")
    public ResponseEntity<byte[]> exportComments(HttpServletRequest req) {
        if (!isAdmin(req)) return ResponseEntity.status(403).build();
        List<Comment> list = commentMapper.selectList(null);
        String csv = "ID,文章ID,昵称,内容,点赞,创建时间\n" + list.stream()
                .map(c -> String.join(",", String.valueOf(c.getId()), String.valueOf(c.getPostId()),
                        escape(c.getNickname()), escape(c.getContent()),
                        String.valueOf(c.getLikes() != null ? c.getLikes() : 0),
                        c.getCreatedAt() != null ? c.getCreatedAt().toString() : ""))
                .collect(Collectors.joining("\n"));
        return csvResponse("comments_" + LocalDate.now() + ".csv", csv);
    }

    private String escape(String s) {
        if (s == null) return "";
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }

    private ResponseEntity<byte[]> csvResponse(String filename, String csv) {
        // 用 GBK 编码 — 中文 Windows Excel 默认以 GBK 打开 CSV，中文正常显示
        byte[] bytes = csv.getBytes(Charset.forName("GBK"));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.parseMediaType("text/csv; charset=GBK"))
                .body(bytes);
    }
}
