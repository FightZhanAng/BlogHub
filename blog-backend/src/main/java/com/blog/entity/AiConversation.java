package com.blog.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.blog.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI 对话表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_conversation")
public class AiConversation extends BaseEntity {

    private Long userId;

    private String title;

    private Long articleId;

    private String modelId;

    private Boolean thinkingEnabled;
}
