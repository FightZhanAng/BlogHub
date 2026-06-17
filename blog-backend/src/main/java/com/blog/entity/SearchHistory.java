package com.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 搜索历史
 */
@Data
@TableName("search_history")
public class SearchHistory {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String keyword;

    private Long userId;

    private LocalDateTime createdAt;
}
