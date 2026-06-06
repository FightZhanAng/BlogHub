package com.blog.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;

/**
 * API 测试环境变量 DTO
 */
@Data
public class ApiTestEnvDTO {

    private Long id;

    @NotBlank(message = "变量名不能为空")
    private String name;

    @NotBlank(message = "变量值不能为空")
    private String value;
}
