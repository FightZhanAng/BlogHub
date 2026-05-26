package com.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.blog.dto.PhotoDTO;
import com.blog.dto.TimelineResponse;
import com.blog.entity.Album;
import com.blog.entity.Photo;
import com.blog.common.enums.ErrorCode;
import com.blog.exception.BusinessException;
import com.blog.exception.ResourceNotFoundException;
import com.blog.mapper.AlbumMapper;
import com.blog.mapper.PhotoMapper;
import com.blog.service.PhotoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PhotoServiceImpl extends ServiceImpl<PhotoMapper, Photo> implements PhotoService {

    private static final Logger log = LoggerFactory.getLogger(PhotoServiceImpl.class);

    private static final List<String> ALLOWED_TYPES = Arrays.asList(
            "image/jpeg", "image/png", "image/gif", "image/webp",
            "video/mp4", "video/webm", "video/ogg", "video/quicktime");
    private static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024;   // 10MB
    private static final long MAX_VIDEO_SIZE = 200 * 1024 * 1024;  // 200MB

    @Autowired
    private AlbumMapper albumMapper;

    @Value("${upload.dir:../uploads}")
    private String uploadDir;

    @Override
    public IPage<PhotoDTO> listPhotos(Long albumId, int page, int size) {
        LambdaQueryWrapper<Photo> wrapper = new LambdaQueryWrapper<Photo>()
                .eq(Photo::getAlbumId, albumId)
                .orderByAsc(Photo::getSortOrder)
                .orderByDesc(Photo::getTakenAt)
                .orderByDesc(Photo::getCreatedAt);
        IPage<Photo> pageResult = baseMapper.selectPage(new Page<>(page, size), wrapper);
        return pageResult.convert(this::toDTO);
    }

    @Override
    @Transactional
    public List<PhotoDTO> uploadPhotos(Long albumId, MultipartFile[] files, Long userId) {
        Album album = albumMapper.selectById(albumId);
        if (album == null) {
            throw new ResourceNotFoundException("相册不存在");
        }
        if (!album.getAuthorId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        Path basePath = Paths.get(uploadDir).toAbsolutePath().normalize();
        List<PhotoDTO> result = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;
            String contentType = file.getContentType();
            if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
                log.warn("跳过不支持的文件类型: {}", contentType);
                continue;
            }
            boolean isVideo = contentType.startsWith("video/");
            long maxSize = isVideo ? MAX_VIDEO_SIZE : MAX_IMAGE_SIZE;
            if (file.getSize() > maxSize) {
                log.warn("跳过超过大小限制的文件: {} ({} bytes)", file.getOriginalFilename(), file.getSize());
                continue;
            }
            try {
                String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
                Path targetDir = basePath.resolve(dateDir);
                Files.createDirectories(targetDir);

                String ext = getExtension(file.getOriginalFilename());
                String filename = UUID.randomUUID().toString().replace("-", "") + "." + ext;
                Path targetPath = targetDir.resolve(filename);
                file.transferTo(targetPath.toFile());

                String url = "/uploads/" + dateDir + "/" + filename;

                Photo photo = new Photo();
                photo.setAlbumId(albumId);
                photo.setPath(url);
                photo.setFilename(file.getOriginalFilename());
                photo.setSize(file.getSize());
                photo.setTitle("");
                photo.setDescription("");
                photo.setTakenAt(LocalDateTime.now());
                photo.setSortOrder(0);
                baseMapper.insert(photo);

                // 更新相册照片数和封面
                int count = baseMapper.countByAlbumId(albumId);
                album.setPhotoCount(count);
                if (album.getCoverImage() == null || album.getCoverImage().isEmpty()) {
                    album.setCoverImage(url);
                }
                albumMapper.updateById(album);

                result.add(toDTO(photo));
                log.info("照片上传成功: albumId={}, url={}", albumId, url);
            } catch (IOException e) {
                log.error("照片上传失败: {}", file.getOriginalFilename(), e);
            }
        }
        return result;
    }

    @Override
    @Transactional
    public PhotoDTO updatePhoto(Long id, String title, String description, String takenAt, Long userId) {
        Photo photo = baseMapper.selectById(id);
        if (photo == null) {
            throw new ResourceNotFoundException("照片不存在");
        }
        Album album = albumMapper.selectById(photo.getAlbumId());
        if (album == null || !album.getAuthorId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (title != null) photo.setTitle(title);
        if (description != null) photo.setDescription(description);
        if (takenAt != null) {
            photo.setTakenAt(LocalDateTime.parse(takenAt, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        baseMapper.updateById(photo);
        return toDTO(photo);
    }

    @Override
    @Transactional
    public void deletePhoto(Long id, Long userId, String role) {
        Photo photo = baseMapper.selectById(id);
        if (photo == null) {
            throw new ResourceNotFoundException("照片不存在");
        }
        Album album = albumMapper.selectById(photo.getAlbumId());
        if (album == null) {
            throw new ResourceNotFoundException("相册不存在");
        }
        if (!album.getAuthorId().equals(userId) && !"admin".equals(role)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        // 删除物理文件
        try {
            Path basePath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path filePath = basePath.resolve(photo.getPath().replace("/uploads/", ""));
            Files.deleteIfExists(filePath);
        } catch (Exception e) {
            log.warn("删除照片文件失败: {}", photo.getPath());
        }
        baseMapper.deleteById(id);
        // 更新相册照片数
        album.setPhotoCount(baseMapper.countByAlbumId(photo.getAlbumId()));
        // 如果删的是封面，重置封面
        if (photo.getPath().equals(album.getCoverImage())) {
            LambdaQueryWrapper<Photo> wrapper = new LambdaQueryWrapper<Photo>()
                    .eq(Photo::getAlbumId, photo.getAlbumId())
                    .orderByDesc(Photo::getCreatedAt)
                    .last("LIMIT 1");
            Photo nextCover = baseMapper.selectOne(wrapper);
            album.setCoverImage(nextCover != null ? nextCover.getPath() : "");
        }
        albumMapper.updateById(album);
        log.info("删除照片: id={}, albumId={}", id, photo.getAlbumId());
    }

    @Override
    public TimelineResponse getTimeline(Long albumId) {
        Album album = albumMapper.selectById(albumId);
        if (album == null) {
            throw new ResourceNotFoundException("相册不存在");
        }
        List<Photo> photos = baseMapper.selectList(
                new LambdaQueryWrapper<Photo>()
                        .eq(Photo::getAlbumId, albumId)
                        .orderByAsc(Photo::getTakenAt));

        TimelineResponse response = new TimelineResponse();
        response.setBabyName(album.getBabyName());
        response.setBirthDate(album.getBabyBirthDate());

        if (album.getBabyBirthDate() == null || photos.isEmpty()) {
            // 没有出生日期或没有照片，按年月分组
            response.setGroups(groupByYearMonth(photos));
            return response;
        }

        // 按月龄分组
        LocalDate birth = album.getBabyBirthDate();
        Map<Integer, List<Photo>> grouped = new TreeMap<>();
        for (Photo p : photos) {
            LocalDateTime taken = p.getTakenAt() != null ? p.getTakenAt() : p.getCreatedAt();
            if (taken == null) continue;
            int months = (taken.getYear() - birth.getYear()) * 12
                    + taken.getMonthValue() - birth.getMonthValue();
            if (months < 0) months = 0;
            grouped.computeIfAbsent(months, k -> new ArrayList<>()).add(p);
        }

        List<TimelineResponse.TimelineGroup> groups = new ArrayList<>();
        for (Map.Entry<Integer, List<Photo>> entry : grouped.entrySet()) {
            int monthAge = entry.getKey();
            TimelineResponse.TimelineGroup group = new TimelineResponse.TimelineGroup();
            group.setMonthAge(monthAge);
            if (monthAge == 0) {
                group.setLabel("出生");
            } else {
                group.setLabel("出生 " + monthAge + " 个月");
            }
            // 计算日期范围
            LocalDate start = birth.plusMonths(monthAge);
            LocalDate end = start.plusMonths(1).minusDays(1);
            group.setDateRange(start.format(DateTimeFormatter.ofPattern("yyyy.MM"))
                    + " - " + end.format(DateTimeFormatter.ofPattern("yyyy.MM")));
            group.setPhotos(entry.getValue().stream().map(this::toDTO).collect(Collectors.toList()));
            groups.add(group);
        }
        response.setGroups(groups);
        return response;
    }

    private List<TimelineResponse.TimelineGroup> groupByYearMonth(List<Photo> photos) {
        Map<YearMonth, List<Photo>> grouped = new TreeMap<>();
        for (Photo p : photos) {
            LocalDateTime taken = p.getTakenAt() != null ? p.getTakenAt() : p.getCreatedAt();
            if (taken == null) continue;
            grouped.computeIfAbsent(YearMonth.from(taken), k -> new ArrayList<>()).add(p);
        }
        List<TimelineResponse.TimelineGroup> groups = new ArrayList<>();
        for (Map.Entry<YearMonth, List<Photo>> entry : grouped.entrySet()) {
            TimelineResponse.TimelineGroup group = new TimelineResponse.TimelineGroup();
            group.setMonthAge(0);
            group.setLabel(entry.getKey().format(DateTimeFormatter.ofPattern("yyyy年MM月")));
            group.setDateRange(entry.getKey().format(DateTimeFormatter.ofPattern("yyyy.MM")));
            group.setPhotos(entry.getValue().stream().map(this::toDTO).collect(Collectors.toList()));
            groups.add(group);
        }
        return groups;
    }

    private PhotoDTO toDTO(Photo photo) {
        PhotoDTO dto = new PhotoDTO();
        dto.setId(photo.getId());
        dto.setAlbumId(photo.getAlbumId());
        dto.setPath(photo.getPath());
        dto.setFilename(photo.getFilename());
        dto.setSize(photo.getSize());
        dto.setTitle(photo.getTitle());
        dto.setDescription(photo.getDescription());
        dto.setTakenAt(photo.getTakenAt());
        dto.setSortOrder(photo.getSortOrder());
        dto.setCreatedAt(photo.getCreatedAt());
        return dto;
    }

    private String getExtension(String filename) {
        if (filename == null) return "jpg";
        int idx = filename.lastIndexOf('.');
        return idx > 0 ? filename.substring(idx + 1).toLowerCase() : "jpg";
    }
}
