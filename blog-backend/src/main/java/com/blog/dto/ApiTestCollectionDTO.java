package com.blog.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;

/**
 * API 测试集合 DTO
 */
@Data
public class ApiTestCollectionDTO {

    private Long id;

    @NotBlank(message = "集合名称不能为空")
    private String name;

    private String description;

    private String baseUrl;
}
