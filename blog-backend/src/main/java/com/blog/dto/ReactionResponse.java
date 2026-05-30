package com.blog.dto;

import lombok.Data;

@Data
public class ReactionResponse {
    private int likeCount;
    private int dislikeCount;
    private Integer myReaction; // 1=已赞, 0=已踩, null=未操作
}
