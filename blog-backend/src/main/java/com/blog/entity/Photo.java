package com.blog.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.blog.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("photos")
public class Photo extends BaseEntity {

    private Long albumId;
    private String path;
    private String filename;
    private Long size;
    private String title;
    private String description;
    private LocalDateTime takenAt;
    private Integer sortOrder;
}
