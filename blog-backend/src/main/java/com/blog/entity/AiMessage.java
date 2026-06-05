package com.blog.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.blog.common.BaseEntity;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

/**
 * AI 消息表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "ai_message", autoResultMap = true)
public class AiMessage extends BaseEntity {

    private Long conversationId;

    private String role;

    private String content;

    private String thinkingContent;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> images;

    private Integer tokensUsed;
}
