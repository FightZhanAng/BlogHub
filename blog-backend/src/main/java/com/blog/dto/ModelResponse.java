package com.blog.dto;

import lombok.Data;

/**
 * 模型信息响应
 */
@Data
public class ModelResponse {
    private String id;
    private String name;
    private String description;
    private Boolean supportThinking;
    private Boolean supportImage;
}
