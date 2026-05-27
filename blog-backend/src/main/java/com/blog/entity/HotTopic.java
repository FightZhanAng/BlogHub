package com.blog.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.blog.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hot_topics")
public class HotTopic extends BaseEntity {

    private String platform;
    private String title;
    private String url;
    private String hotValue;
    private Integer rankNum;
    private String extra;
    private LocalDate fetchedAt;
}
