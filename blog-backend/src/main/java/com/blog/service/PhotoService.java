package com.blog.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.blog.dto.PhotoDTO;
import com.blog.dto.TimelineResponse;
import com.blog.entity.Photo;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface PhotoService extends IService<Photo> {

    IPage<PhotoDTO> listPhotos(Long albumId, int page, int size);

    List<PhotoDTO> uploadPhotos(Long albumId, MultipartFile[] files, Long userId);

    PhotoDTO updatePhoto(Long id, String title, String description, String takenAt, Long userId);

    void deletePhoto(Long id, Long userId, String role);

    TimelineResponse getTimeline(Long albumId);
}
