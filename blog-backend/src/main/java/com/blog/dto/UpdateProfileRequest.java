package com.blog.dto;

import lombok.Data;

import javax.validation.constraints.Size;

@Data
public class UpdateProfileRequest {

    @Size(max = 50, message = "昵称长度不能超过 50")
    private String nickname;

    @Size(max = 500, message = "头像 URL 长度不能超过 500")
    private String avatar;
}
