package com.blog.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.blog.common.AuditLog;
import com.blog.common.PageResult;
import com.blog.common.Result;
import com.blog.entity.User;
import com.blog.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    @Value("${upload.dir:./uploads}")
    private String uploadDir;

    @Autowired
    private UserService userService;

    @Autowired
    private com.blog.service.PostService postService;

    // ========== 管理员管理用户 ==========

    @GetMapping
    public Result<PageResult<User>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        return Result.success(PageResult.of(userService.getUsers(page, size, keyword)));
    }

    @GetMapping("/me")
    public Result<User> me(@RequestAttribute Long userId) {
        return Result.success(userService.getUserById(userId));
    }

    @PostMapping
    @AuditLog(action = "创建用户", resource = "#body?.get('username')")
    public Result<User> create(@RequestBody Map<String, String> body) {
        return Result.success(userService.createUser(
                body.get("username"), body.get("password"),
                body.get("nickname"), body.get("role")));
    }

    @PutMapping("/{id}")
    @AuditLog(action = "更新用户", resource = "#id")
    public Result<User> update(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return Result.success(userService.updateUser(id, body));
    }

    @DeleteMapping("/{id}")
    @AuditLog(action = "删除用户", resource = "#id")
    public Result<Void> delete(@PathVariable Long id) {
        userService.removeById(id);
        return Result.noContent();
    }

    // ========== 用户搜索（@ 提及用） ==========

    @GetMapping("/search")
    public Result<List<User>> search(@RequestParam String q,
                                      @RequestParam(defaultValue = "8") int limit) {
        if (q == null || q.trim().length() < 1) {
            return Result.success(Collections.emptyList());
        }
        return Result.success(userService.searchByUsername(q.trim(), limit));
    }

    // ========== 头像上传 ==========

    @PostMapping("/me/avatar")
    public Result<Map<String, String>> uploadAvatar(
            @RequestAttribute Long userId,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        if (file.isEmpty()) return Result.error(400, "文件不能为空");
        try {
            String dateDir = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy/MM"));
            Path targetDir = Paths.get(uploadDir).toAbsolutePath().normalize().resolve("avatars").resolve(dateDir);
            java.nio.file.Files.createDirectories(targetDir);
            String ext = file.getOriginalFilename();
            ext = ext != null && ext.contains(".") ? ext.substring(ext.lastIndexOf('.') + 1).toLowerCase() : "png";
            String filename = "avatar_" + userId + "_" + java.util.UUID.randomUUID().toString().substring(0, 8) + "." + ext;
            file.transferTo(targetDir.resolve(filename).toFile());
            String url = "/uploads/avatars/" + dateDir + "/" + filename;
            userService.updateProfile(userId, null, url);
            log.info("头像上传成功: userId={}, url={}", userId, url);
            Map<String, String> r = new HashMap<>();
            r.put("url", url);
            return Result.success(r);
        } catch (java.io.IOException e) {
            log.error("头像上传失败", e);
            return Result.error(500, "头像上传失败");
        }
    }

    // ========== 个人中心 ==========

    @PutMapping("/me")
    public Result<User> updateMe(@RequestAttribute Long userId, @RequestBody Map<String, String> body) {
        return Result.success(userService.updateProfile(userId, body.get("nickname"), body.get("avatar")));
    }

    @PutMapping("/me/password")
    public Result<Void> changePassword(@RequestAttribute Long userId, @RequestBody Map<String, String> body) {
        userService.changePassword(userId, body.get("oldPassword"), body.get("newPassword"));
        return Result.success(null);
    }

    @GetMapping("/me/posts")
    public Result<PageResult<com.blog.entity.Post>> myPosts(
            @RequestAttribute Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(PageResult.of(postService.getUserPosts(userId, page, size)));
    }

    @GetMapping("/me/comments")
    public Result<PageResult<Map<String, Object>>> myComments(
            @RequestAttribute Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(userService.getMyComments(userId, page, size));
    }
}
