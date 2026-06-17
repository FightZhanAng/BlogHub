package com.blog.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.blog.common.PageResult;
import com.blog.common.Result;
import com.blog.config.JwtUtil;
import com.blog.dto.*;
import com.blog.entity.Album;
import com.blog.service.AlbumService;
import com.blog.service.PhotoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/albums")
@Slf4j
@RequiredArgsConstructor
@Tag(name = "宝宝相册", description = "相册和照片管理接口")
public class AlbumController {

    private static final Logger log = LoggerFactory.getLogger(AlbumController.class);

    private final AlbumService albumService;

    private final PhotoService photoService;

    private final JwtUtil jwtUtil;

    private final HttpServletRequest request;

    private Long getCurrentUserId() {
        try {
            String header = request.getHeader("Authorization");
            if (header != null && header.startsWith("Bearer ")) {
                String token = header.substring(7);
                if (jwtUtil.validateToken(token)) return jwtUtil.getUserId(token);
            }
        } catch (Exception e) { log.debug("解析 userId 失败: {}", e.getMessage()); }
        return null;
    }

    private String getCurrentRole() {
        try {
            String header = request.getHeader("Authorization");
            if (header != null && header.startsWith("Bearer ")) {
                String token = header.substring(7);
                if (jwtUtil.validateToken(token)) return jwtUtil.getRole(token);
            }
        } catch (Exception e) { log.debug("解析 role 失败: {}", e.getMessage()); }
        return null;
    }

    // ==================== 相册接口 ====================

    @GetMapping
    @Operation(summary = "相册列表")
    public Result<PageResult<AlbumResponse>> listAlbums(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size) {
        Long userId = getCurrentUserId();
        IPage<AlbumResponse> result = albumService.listAlbums(page, size, userId);
        return Result.success(PageResult.of(result));
    }

    @GetMapping("/{id}")
    @Operation(summary = "相册详情")
    public Result<AlbumResponse> getAlbum(@PathVariable Long id) {
        AlbumResponse album = albumService.getAlbumDetail(id);
        return Result.success(album);
    }

    @PostMapping
    @Operation(summary = "创建相册")
    public Result<Album> createAlbum(@Valid @RequestBody AlbumDTO dto) {
        Long userId = getCurrentUserId();
        if (userId == null) return Result.error(401, "请先登录");
        Album album = albumService.createAlbum(dto, userId);
        return Result.created(album);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新相册")
    public Result<Album> updateAlbum(@PathVariable Long id, @Valid @RequestBody AlbumDTO dto) {
        Long userId = getCurrentUserId();
        if (userId == null) return Result.error(401, "请先登录");
        String role = getCurrentRole();
        Album album = albumService.updateAlbum(id, dto, userId, role);
        return Result.success(album);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除相册")
    public Result<Void> deleteAlbum(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        if (userId == null) return Result.error(401, "请先登录");
        String role = getCurrentRole();
        albumService.deleteAlbum(id, userId, role);
        return Result.success();
    }

    // ==================== 照片接口 ====================

    @GetMapping("/{albumId}/photos")
    @Operation(summary = "照片列表")
    public Result<PageResult<PhotoDTO>> listPhotos(
            @PathVariable Long albumId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size) {
        IPage<PhotoDTO> result = photoService.listPhotos(albumId, page, size);
        return Result.success(PageResult.of(result));
    }

    @PostMapping("/{albumId}/photos")
    @Operation(summary = "批量上传照片")
    public Result<List<PhotoDTO>> uploadPhotos(
            @PathVariable Long albumId,
            @RequestParam("files") MultipartFile[] files) {
        Long userId = getCurrentUserId();
        if (userId == null) return Result.error(401, "请先登录");
        List<PhotoDTO> result = photoService.uploadPhotos(albumId, files, userId);
        return Result.success(result);
    }

    @PutMapping("/photos/{id}")
    @Operation(summary = "更新照片信息")
    public Result<PhotoDTO> updatePhoto(
            @PathVariable Long id,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String takenAt) {
        Long userId = getCurrentUserId();
        if (userId == null) return Result.error(401, "请先登录");
        PhotoDTO photo = photoService.updatePhoto(id, title, description, takenAt, userId);
        return Result.success(photo);
    }

    @DeleteMapping("/photos/{id}")
    @Operation(summary = "删除照片")
    public Result<Void> deletePhoto(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        if (userId == null) return Result.error(401, "请先登录");
        String role = getCurrentRole();
        photoService.deletePhoto(id, userId, role);
        return Result.success();
    }

    // ==================== 时间线接口 ====================

    @GetMapping("/{id}/timeline")
    @Operation(summary = "成长时间线")
    public Result<TimelineResponse> getTimeline(@PathVariable Long id) {
        TimelineResponse timeline = photoService.getTimeline(id);
        return Result.success(timeline);
    }
}
