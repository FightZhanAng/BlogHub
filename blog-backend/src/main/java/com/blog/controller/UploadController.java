package com.blog.controller;

import com.blog.common.Result;
import com.blog.entity.Image;
import com.blog.mapper.ImageMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/upload")
public class UploadController {

    private static final Logger log = LoggerFactory.getLogger(UploadController.class);

    private static final List<String> ALLOWED_TYPES = Arrays.asList(
            "image/jpeg", "image/png", "image/gif", "image/webp", "image/svg+xml");
    private static final long MAX_SIZE = 5 * 1024 * 1024; // 5MB

    @Autowired
    private ImageMapper imageMapper;

    @Value("${upload.dir:./uploads}")
    private String uploadDir;

    private Path uploadPath;

    @PostConstruct
    public void init() throws IOException {
        uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(uploadPath);
    }

    @PostMapping
    public Result<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error(400, "文件不能为空");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            return Result.error(400, "不支持的文件类型，仅允许 jpg/png/gif/webp/svg");
        }

        if (file.getSize() > MAX_SIZE) {
            return Result.error(400, "文件大小不能超过 5MB");
        }

        try {
            // 按日期分目录存储
            String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
            Path targetDir = uploadPath.resolve(dateDir);
            Files.createDirectories(targetDir);

            // 生成唯一文件名
            String ext = getExtension(file.getOriginalFilename());
            String filename = UUID.randomUUID().toString().replace("-", "") + "." + ext;
            Path targetPath = targetDir.resolve(filename);
            file.transferTo(targetPath.toFile());

            String url = "/uploads/" + dateDir + "/" + filename;
            log.info("文件上传成功: {} ({} bytes)", url, file.getSize());

            // 记录到 images 表
            try {
                Image img = new Image();
                img.setPath(url);
                img.setFilename(file.getOriginalFilename());
                img.setSize(file.getSize());
                img.setType("unknown");
                imageMapper.insert(img);
            } catch (Exception e) {
                log.warn("记录图片到数据库失败: {}", e.getMessage());
            }

            Map<String, String> result = new HashMap<>();
            result.put("url", url);
            return Result.success(result);
        } catch (IOException e) {
            log.error("文件上传失败", e);
            return Result.error(500, "文件上传失败: " + e.getMessage());
        }
    }

    private String getExtension(String filename) {
        if (filename == null) return "png";
        int idx = filename.lastIndexOf('.');
        return idx > 0 ? filename.substring(idx + 1).toLowerCase() : "png";
    }
}
