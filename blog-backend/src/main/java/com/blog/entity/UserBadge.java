package com.blog.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户徽章实体
 */
@Data
@TableName("user_badge")
public class UserBadge {

    private Long id;
    private Long userId;
    private Long badgeId;
    private LocalDateTime earnedAt;
    private Integer isNew;
}
