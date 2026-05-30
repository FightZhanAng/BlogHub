package com.blog.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class ReactionRequest {

    @NotNull(message = "type 不能为空")
    private Integer type; // 1=赞, 0=踩
}
