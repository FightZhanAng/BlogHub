package com.blog.dto;

import lombok.Data;

/**
 * 图片上传响应
 */
@Data
public class ImageUploadResponse {
    private Long id;
    private String url;
    private String mimeType;
    private Long size;
}
