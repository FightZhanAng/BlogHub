package com.blog.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class RefreshRequest {

    @NotBlank(message = "Refresh Token 不能为空")
    private String refreshToken;
}
