package com.blog.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.blog.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("albums")
public class Album extends BaseEntity {

    private String title;
    private String description;
    private String coverImage;
    private String babyName;
    private LocalDate babyBirthDate;
    private Long authorId;
    private Integer photoCount;
    private Boolean isPrivate;
}
