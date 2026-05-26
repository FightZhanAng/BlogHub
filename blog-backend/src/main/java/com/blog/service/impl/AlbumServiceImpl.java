package com.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.blog.dto.AlbumDTO;
import com.blog.dto.AlbumResponse;
import com.blog.entity.Album;
import com.blog.entity.Photo;
import com.blog.common.enums.ErrorCode;
import com.blog.exception.BusinessException;
import com.blog.exception.ResourceNotFoundException;
import com.blog.mapper.AlbumMapper;
import com.blog.mapper.PhotoMapper;
import com.blog.service.AlbumService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AlbumServiceImpl extends ServiceImpl<AlbumMapper, Album> implements AlbumService {

    private static final Logger log = LoggerFactory.getLogger(AlbumServiceImpl.class);

    @Autowired
    private PhotoMapper photoMapper;

    @Value("${upload.dir:../uploads}")
    private String uploadDir;

    @Override
    public IPage<AlbumResponse> listAlbums(int page, int size, Long authorId) {
        LambdaQueryWrapper<Album> wrapper = new LambdaQueryWrapper<>();
        if (authorId != null) {
            wrapper.eq(Album::getAuthorId, authorId);
        }
        wrapper.orderByDesc(Album::getCreatedAt);

        IPage<Album> pageResult = baseMapper.selectPage(new Page<>(page, size), wrapper);
        return pageResult.convert(album -> {
            AlbumResponse r = AlbumResponse.from(album);
            // 补充照片数
            r.setPhotoCount(photoMapper.countByAlbumId(album.getId()));
            return r;
        });
    }

    @Override
    public AlbumResponse getAlbumDetail(Long id) {
        Album album = baseMapper.selectById(id);
        if (album == null) {
            throw new ResourceNotFoundException("相册不存在");
        }
        AlbumResponse r = AlbumResponse.from(album);
        r.setPhotoCount(photoMapper.countByAlbumId(id));
        return r;
    }

    @Override
    @Transactional
    public Album createAlbum(AlbumDTO dto, Long authorId) {
        Album album = new Album();
        album.setTitle(dto.getTitle());
        album.setDescription(dto.getDescription());
        album.setCoverImage(dto.getCoverImage());
        album.setBabyName(dto.getBabyName());
        album.setBabyBirthDate(dto.getBabyBirthDate());
        album.setAuthorId(authorId);
        album.setPhotoCount(0);
        album.setIsPrivate(dto.getIsPrivate() != null ? dto.getIsPrivate() : true);
        baseMapper.insert(album);
        log.info("创建相册: id={}, title={}", album.getId(), album.getTitle());
        return album;
    }

    @Override
    @Transactional
    public Album updateAlbum(Long id, AlbumDTO dto, Long userId, String role) {
        Album album = baseMapper.selectById(id);
        if (album == null) {
            throw new ResourceNotFoundException("相册不存在");
        }
        if (!album.getAuthorId().equals(userId) && !"admin".equals(role)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        album.setTitle(dto.getTitle());
        album.setDescription(dto.getDescription());
        if (dto.getCoverImage() != null) album.setCoverImage(dto.getCoverImage());
        if (dto.getBabyName() != null) album.setBabyName(dto.getBabyName());
        if (dto.getBabyBirthDate() != null) album.setBabyBirthDate(dto.getBabyBirthDate());
        if (dto.getIsPrivate() != null) album.setIsPrivate(dto.getIsPrivate());
        baseMapper.updateById(album);
        log.info("更新相册: id={}", id);
        return album;
    }

    @Override
    @Transactional
    public void deleteAlbum(Long id, Long userId, String role) {
        Album album = baseMapper.selectById(id);
        if (album == null) {
            throw new ResourceNotFoundException("相册不存在");
        }
        if (!album.getAuthorId().equals(userId) && !"admin".equals(role)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        // 删除相册下所有照片的物理文件
        List<Photo> photos = photoMapper.selectList(
                new LambdaQueryWrapper<Photo>().eq(Photo::getAlbumId, id));
        Path basePath = Paths.get(uploadDir).toAbsolutePath().normalize();
        for (Photo photo : photos) {
            try {
                Path filePath = basePath.resolve(photo.getPath().replace("/uploads/", ""));
                Files.deleteIfExists(filePath);
            } catch (Exception e) {
                log.warn("删除照片文件失败: {}", photo.getPath());
            }
        }
        // 删除照片记录
        photoMapper.delete(new LambdaQueryWrapper<Photo>().eq(Photo::getAlbumId, id));
        // 删除相册
        baseMapper.deleteById(id);
        log.info("删除相册及照片: albumId={}, photoCount={}", id, photos.size());
    }
}
