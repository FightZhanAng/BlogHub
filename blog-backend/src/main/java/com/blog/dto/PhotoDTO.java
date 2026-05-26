package com.blog.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PhotoDTO {

    private Long id;
    private Long albumId;
    private String path;
    private String filename;
    private Long size;
    private String title;
    private String description;
    private LocalDateTime takenAt;
    private Integer sortOrder;
    private LocalDateTime createdAt;
}
