package com.blog.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class FollowRequest {

    @NotNull(message = "用户 ID 不能为空")
    private Long userId;
}
