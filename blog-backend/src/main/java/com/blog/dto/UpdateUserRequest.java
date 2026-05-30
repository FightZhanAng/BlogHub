package com.blog.dto;

import lombok.Data;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
public class UpdateUserRequest {

    @Size(max = 50, message = "昵称长度不能超过 50")
    private String nickname;

    @Pattern(regexp = "^(admin|user)$", message = "角色值无效，仅允许 admin 或 user")
    private String role;

    @Pattern(regexp = "^[01]$", message = "状态值无效，仅允许 0 或 1")
    private String status;

    @Size(min = 6, max = 100, message = "密码长度 6-100 个字符")
    private String password;
}
