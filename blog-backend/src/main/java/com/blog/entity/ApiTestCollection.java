package com.blog.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.blog.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * API 测试集合表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("api_test_collection")
public class ApiTestCollection extends BaseEntity {

    private String name;

    private String description;

    private Long userId;

    private String baseUrl;
}
