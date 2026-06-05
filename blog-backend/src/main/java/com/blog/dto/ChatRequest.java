package com.blog.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

/**
 * AI 聊天请求
 */
@Data
public class ChatRequest {

    @NotNull(message = "对话ID不能为空")
    private Long conversationId;

    @NotNull(message = "消息内容不能为空")
    private String message;

    private String modelId;

    private Boolean thinkingEnabled;

    private List<String> imageUrls;

    private List<String> imageBase64s;

    private List<Map<String, String>> history;
}
