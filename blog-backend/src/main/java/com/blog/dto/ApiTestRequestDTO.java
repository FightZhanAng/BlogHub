package com.blog.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;

/**
 * API 测试请求 DTO
 */
@Data
public class ApiTestRequestDTO {

    private Long id;

    private Long collectionId;

    @NotBlank(message = "请求名称不能为空")
    private String name;

    @NotBlank(message = "请求方法不能为空")
    private String method;

    @NotBlank(message = "请求 URL 不能为空")
    private String url;

    private String headers; // JSON string

    private String bodyType;

    private String body;
}
