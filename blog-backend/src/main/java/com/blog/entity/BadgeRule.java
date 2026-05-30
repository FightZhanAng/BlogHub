package com.blog.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.blog.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 徽章规则实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("badge_rule")
public class BadgeRule extends BaseEntity {

    /** 关联徽章 ID */
    private Long badgeId;

    /** 触发事件类型 */
    private String eventType;

    /** 条件 JSON */
    private String conditionJson;
}
