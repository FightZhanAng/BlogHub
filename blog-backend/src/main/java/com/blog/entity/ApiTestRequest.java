package com.blog.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.blog.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * API 测试请求表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("api_test_request")
public class ApiTestRequest extends BaseEntity {

    private Long collectionId;

    private Long userId;

    private String name;

    private String method;

    private String url;

    private String headers;

    private String bodyType;

    private String body;
}
