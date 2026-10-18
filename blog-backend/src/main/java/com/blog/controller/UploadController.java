package com.blog.controller;

import com.blog.common.Result;
import com.blog.entity.Image;
import com.blog.mapper.ImageMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
@Tag(name = "文件上传", description = "图片文件上传")
public class UploadController {

    private static final Logger log = LoggerFactory.getLogger(UploadController.class);

    private static final List<String> ALLOWED_TYPES = Arrays.asList(
            "image/jpeg", "image/png", "image/gif", "image/webp", "image/bmp");
    private static final long MAX_SIZE = com.blog.common.UploadValidator.MAX_IMAGE_SIZE;

    private final ImageMapper imageMapper;

    @Value("${upload.dir:./uploads}")
    private String uploadDir;

    private Path uploadPath;

    @PostConstruct
    public void init() throws IOException {
        uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(uploadPath);
    }

    @Operation(summary = "上传图片文件")
    @PostMapping
    public Result<Map<String, String>> upload(@RequestParam("file") MultipartFile file,
                                              HttpServletRequest request) {
        // 之前 /api/upload/** 匿名放行且方法内无鉴权：任何人可无限写盘，
        // 配合 svg 白名单还能在 /uploads 下同源执行脚本。
        Object userId = request.getAttribute("userId");
        if (!(userId instanceof Long)) {
            return Result.error(401, "请先登录后再上传");
        }
        if (file.isEmpty()) {
            return Result.error(400, "文件不能为空");
        }

        if (file.getSize() > MAX_SIZE) {
            return Result.error(400, "文件大小不能超过 5MB");
        }

        try {
            // 只信文件头魔数，不信客户端声明的 Content-Type 与文件扩展名
            byte[] header = new byte[12];
            try (java.io.InputStream in = file.getInputStream()) {
                int read = 0;
                while (read < header.length) {
                    int n = in.read(header, read, header.length - read);
                    if (n < 0) break;
                    read += n;
                }
            }
            String ext = com.blog.common.UploadValidator.detectImageExtension(header);
            if (ext == null) {
                return Result.error(400, "文件内容不是受支持的图片格式（仅允许 jpg/png/gif/webp/bmp）");
            }

            // 按日期分目录存储
            String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
            Path targetDir = uploadPath.resolve(dateDir);
            Files.createDirectories(targetDir);

            // 生成唯一文件名
            String filename = UUID.randomUUID().toString().replace("-", "") + "." + ext;
            Path targetPath = targetDir.resolve(filename);
            file.transferTo(targetPath.toFile());

            String url = "/uploads/" + dateDir + "/" + filename;
            log.info("文件上传成功: {} ({} bytes, user={})", url, file.getSize(), userId);

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
            return Result.error(500, "文件上传失败");
        }
    }
}
