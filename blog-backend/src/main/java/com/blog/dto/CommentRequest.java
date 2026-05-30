package com.blog.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 发表评论请求
 */
@Data
public class CommentRequest {

    @NotBlank(message = "昵称不能为空")
    @Size(max = 50, message = "昵称长度不能超过 50")
    private String nickname;

    @NotBlank(message = "评论内容不能为空")
    @Size(max = 500, message = "评论内容长度不能超过 500")
    private String content;

    /** 父评论 ID，为空则为顶级评论 */
    private Long parentId;
}
