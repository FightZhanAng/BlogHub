package com.blog.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.blog.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI 图片表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_image")
public class AiImage extends BaseEntity {

    private Long userId;

    private String filename;

    private String originalName;

    private String mimeType;

    private Long size;

    private String url;
}
