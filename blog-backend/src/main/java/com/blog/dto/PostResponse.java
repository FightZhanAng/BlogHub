package com.blog.dto;

import com.blog.entity.Post;

import java.time.LocalDateTime;

/**
 * 文章响应（脱敏，不暴露内部字段）
 */
public class PostResponse {

    private Long id;
    private String slug;
    private Integer isPinned;
    private String authorAvatar;
    private String title;
    private String description;
    private String content;
    private String tags;
    private Integer status;
    private Integer views;
    private Integer likes;
    private String coverImage;
    private String scheduledAt;
    private Long seriesId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String authorName;
    private Long authorId;

    public static PostResponse from(Post post) {
        if (post == null) return null;
        PostResponse r = new PostResponse();
        r.setId(post.getId());
        r.setSlug(post.getSlug());
        r.setTitle(post.getTitle());
        r.setDescription(post.getDescription());
        r.setContent(post.getContent());
        r.setTags(post.getTags());
        r.setStatus(post.getStatus());
        r.setViews(post.getViews());
        r.setLikes(post.getLikes());
        r.setCoverImage(post.getCoverImage());
        r.setCreatedAt(post.getCreatedAt());
        r.setUpdatedAt(post.getUpdatedAt());
        r.setAuthorName(post.getAuthorName());
        r.setAuthorId(post.getAuthorId());
        r.setIsPinned(post.getIsPinned());
        return r;
    }

    // ========== Getters / Setters ==========

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Integer getViews() { return views; }
    public void setViews(Integer views) { this.views = views; }
    public Integer getLikes() { return likes; }
    public void setLikes(Integer likes) { this.likes = likes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    public Long getAuthorId() { return authorId; }
    public void setAuthorId(Long authorId) { this.authorId = authorId; }
    public Integer getIsPinned() { return isPinned; }
    public void setIsPinned(Integer isPinned) { this.isPinned = isPinned; }
    public String getAuthorAvatar() { return authorAvatar; }
    public void setAuthorAvatar(String authorAvatar) { this.authorAvatar = authorAvatar; }
    public String getCoverImage() { return coverImage; }
    public void setCoverImage(String coverImage) { this.coverImage = coverImage; }
    public String getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(String scheduledAt) { this.scheduledAt = scheduledAt; }
    public Long getSeriesId() { return seriesId; }
    public void setSeriesId(Long seriesId) { this.seriesId = seriesId; }
}
