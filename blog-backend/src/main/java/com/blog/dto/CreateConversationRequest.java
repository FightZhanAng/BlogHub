package com.blog.dto;

import lombok.Data;

/**
 * 创建对话请求
 */
@Data
public class CreateConversationRequest {
    private String title;
    private String modelId;
    private Boolean thinkingEnabled;
    private Long articleId;
}
