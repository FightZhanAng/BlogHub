package com.blog.controller;

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
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 鏁版嵁瀵煎嚭鎺ュ彛锛堜粎绠＄悊鍛橈紝CSV 鏍煎紡锛? */
@RestController
@RequestMapping("/api/export")
@RequiredArgsConstructor
@Tag(name = "鏁版嵁瀵煎嚭", description = "CSV鏍煎紡鏁版嵁瀵煎嚭")
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
        } catch (Exception e) { log.debug("操作失败: {}", e.getMessage()); }
        return false;
    }

    @Operation(summary = "瀵煎嚭鏂囩珷鏁版嵁")
    @GetMapping("/posts")
    public ResponseEntity<byte[]> exportPosts(HttpServletRequest req) {
        if (!isAdmin(req)) return ResponseEntity.status(403).build();
        List<Post> list = postMapper.selectList(null);
        String csv = "ID,鏍囬,Slug,闃呰閲?鐐硅禐,鐘舵€?鍒涘缓鏃堕棿\n" + list.stream()
                .map(p -> String.join(",", String.valueOf(p.getId()), escape(p.getTitle()), p.getSlug(),
                        String.valueOf(p.getViews() != null ? p.getViews() : 0),
                        String.valueOf(p.getLikes() != null ? p.getLikes() : 0),
                        String.valueOf(p.getStatus()),
                        p.getCreatedAt() != null ? p.getCreatedAt().toString() : ""))
                .collect(Collectors.joining("\n"));
        return csvResponse("posts_" + LocalDate.now() + ".csv", csv);
    }

    @Operation(summary = "瀵煎嚭鐢ㄦ埛鏁版嵁")
    @GetMapping("/users")
    public ResponseEntity<byte[]> exportUsers(HttpServletRequest req) {
        if (!isAdmin(req)) return ResponseEntity.status(403).build();
        List<User> list = userMapper.selectList(null);
        String csv = "ID,鐢ㄦ埛鍚?鏄电О,瑙掕壊,鐘舵€?鍒涘缓鏃堕棿\n" + list.stream()
                .map(u -> String.join(",", String.valueOf(u.getId()), u.getUsername(),
                        u.getNickname() != null ? escape(u.getNickname()) : "",
                        u.getRole(), String.valueOf(u.getStatus()),
                        u.getCreatedAt() != null ? u.getCreatedAt().toString() : ""))
                .collect(Collectors.joining("\n"));
        return csvResponse("users_" + LocalDate.now() + ".csv", csv);
    }

    @Operation(summary = "瀵煎嚭璇勮鏁版嵁")
    @GetMapping("/comments")
    public ResponseEntity<byte[]> exportComments(HttpServletRequest req) {
        if (!isAdmin(req)) return ResponseEntity.status(403).build();
        List<Comment> list = commentMapper.selectList(null);
        String csv = "ID,鏂囩珷ID,鏄电О,鍐呭,鐐硅禐,鍒涘缓鏃堕棿\n" + list.stream()
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
        // 鐢?GBK 缂栫爜 鈥?涓枃 Windows Excel 榛樿浠?GBK 鎵撳紑 CSV锛屼腑鏂囨甯告樉绀?        byte[] bytes = csv.getBytes(Charset.forName("GBK"));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.parseMediaType("text/csv; charset=GBK"))
                .body(bytes);
    }
}
