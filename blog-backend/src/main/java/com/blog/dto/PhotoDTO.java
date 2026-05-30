package com.blog.dto;

import lombok.Data;

import javax.validation.constraints.Size;
import java.time.LocalDateTime;

@Data
public class PhotoDTO {

    private Long id;
    private Long albumId;
    private String path;
    private String filename;
    private Long size;

    @Size(max = 100, message = "照片标题长度不能超过 100")
    private String title;

    @Size(max = 500, message = "照片描述长度不能超过 500")
    private String description;

    private LocalDateTime takenAt;
    private Integer sortOrder;
    private LocalDateTime createdAt;
}
