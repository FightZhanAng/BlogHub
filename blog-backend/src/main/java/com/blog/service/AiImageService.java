package com.blog.service;

import com.blog.dto.ImageUploadResponse;
import com.blog.entity.AiImage;
import com.blog.mapper.AiImageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * AI 图片上传服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiImageService {

    private final AiImageMapper aiImageMapper;

    @Value("${upload.dir:./uploads}")
    private String uploadDir;

    private static final long MAX_SIZE = 50 * 1024 * 1024; // 50MB
    private static final String[] ALLOWED_TYPES = {
            "image/jpeg", "image/png", "image/gif", "image/webp", "image/bmp"
    };

    /**
     * 上传图片
     */
    public ImageUploadResponse uploadImage(MultipartFile file, Long userId) throws Exception {
        log.info("收到图片上传: filename={}, size={}, contentType={}", 
                file.getOriginalFilename(), file.getSize(), file.getContentType());
        // 校验文件
        if (file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new IllegalArgumentException("图片大小不能超过 50MB");
        }
        String contentType = file.getContentType();
        boolean allowed = false;
        for (String t : ALLOWED_TYPES) {
            if (t.equals(contentType)) { allowed = true; break; }
        }
        if (!allowed) {
            throw new IllegalArgumentException("不支持的图片格式");
        }

        // 保存文件
        String dateDir = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy/MM"));
        Path targetDir = Paths.get(uploadDir).toAbsolutePath().normalize().resolve("ai-images").resolve(dateDir);
        log.info("保存目录: {}", targetDir);
        Files.createDirectories(targetDir);

        String ext = file.getOriginalFilename();
        ext = ext != null && ext.contains(".") ? ext.substring(ext.lastIndexOf('.') + 1).toLowerCase() : "png";
        String filename = "ai_" + UUID.randomUUID().toString().substring(0, 8) + "." + ext;
        file.transferTo(targetDir.resolve(filename).toFile());

        String url = "/uploads/ai-images/" + dateDir + "/" + filename;

        // 保存记录
        AiImage aiImage = new AiImage();
        aiImage.setUserId(userId);
        aiImage.setFilename(filename);
        aiImage.setOriginalName(file.getOriginalFilename());
        aiImage.setMimeType(contentType);
        aiImage.setSize(file.getSize());
        aiImage.setUrl(url);
        aiImageMapper.insert(aiImage);

        log.info("AI图片上传成功: userId={}, url={}", userId, url);

        ImageUploadResponse resp = new ImageUploadResponse();
        resp.setId(aiImage.getId());
        resp.setUrl(url);
        resp.setMimeType(contentType);
        resp.setSize(file.getSize());
        return resp;
    }
}
