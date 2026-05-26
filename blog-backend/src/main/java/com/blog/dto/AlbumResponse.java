package com.blog.dto;

import com.blog.entity.Album;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class AlbumResponse {

    private Long id;
    private String title;
    private String description;
    private String coverImage;
    private String babyName;
    private LocalDate babyBirthDate;
    private Long authorId;
    private Integer photoCount;
    private Boolean isPrivate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 详情页附带照片列表
    private List<PhotoDTO> photos;

    public static AlbumResponse from(Album album) {
        AlbumResponse r = new AlbumResponse();
        r.setId(album.getId());
        r.setTitle(album.getTitle());
        r.setDescription(album.getDescription());
        r.setCoverImage(album.getCoverImage());
        r.setBabyName(album.getBabyName());
        r.setBabyBirthDate(album.getBabyBirthDate());
        r.setAuthorId(album.getAuthorId());
        r.setPhotoCount(album.getPhotoCount());
        r.setIsPrivate(album.getIsPrivate());
        r.setCreatedAt(album.getCreatedAt());
        r.setUpdatedAt(album.getUpdatedAt());
        return r;
    }
}
