package com.blog.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
public class CreateUserRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度 3-20 个字符")
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_]{2,19}$",
            message = "用户名必须以字母开头，只能包含字母、数字、下划线")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 100, message = "密码长度 6-100 个字符")
    private String password;

    @Size(max = 50, message = "昵称长度不能超过 50")
    private String nickname;

    @Size(max = 20, message = "角色长度不能超过 20")
    private String role;
}
