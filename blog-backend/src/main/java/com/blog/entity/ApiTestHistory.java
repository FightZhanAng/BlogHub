package com.blog.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.blog.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * API 测试历史记录表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("api_test_history")
public class ApiTestHistory extends BaseEntity {

    private Long userId;

    private String method;

    private String url;

    private String headers;

    private String bodyType;

    private String body;

    private Integer statusCode;

    private Integer responseTime;

    private Integer responseSize;

    private String responseBody;

    private String responseHeaders;
}
