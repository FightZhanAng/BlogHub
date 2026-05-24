package com.blog.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;

@TableName("daily_views")
public class DailyViews {
    private Long id;
    private Long postId;
    private LocalDate date;
    private Integer views;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPostId() { return postId; }
    public void setPostId(Long postId) { this.postId = postId; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public Integer getViews() { return views; }
    public void setViews(Integer views) { this.views = views; }
}
