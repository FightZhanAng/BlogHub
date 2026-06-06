package com.blog.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.blog.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * API 测试环境变量表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("api_test_env")
public class ApiTestEnv extends BaseEntity {

    private Long userId;

    private String name;

    private String value;
}
