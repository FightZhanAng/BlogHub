package com.blog.dto;

import com.blog.entity.Comment;

/**
 * 评论 DTO
 */
public class CommentDTO {

    private Long id;
    private Long postId;
    private Long parentId;
    private String nickname;
    private String content;
    private Integer likes;
    private Integer dislikes;
    private String createdAt;
    private String postTitle;
    private String postSlug;

    public static CommentDTO from(Comment comment) {
        if (comment == null) return null;
        CommentDTO dto = new CommentDTO();
        dto.setId(comment.getId());
        dto.setPostId(comment.getPostId());
        dto.setParentId(comment.getParentId());
        dto.setNickname(comment.getNickname());
        dto.setContent(comment.getContent());
        dto.setLikes(comment.getLikes() != null ? comment.getLikes() : 0);
        dto.setDislikes(comment.getDislikes() != null ? comment.getDislikes() : 0);
        dto.setCreatedAt(comment.getCreatedAt() != null ? comment.getCreatedAt().toString() : null);
        return dto;
    }

    // ====== Getters / Setters ======

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPostId() { return postId; }
    public void setPostId(Long postId) { this.postId = postId; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Integer getLikes() { return likes; }
    public void setLikes(Integer likes) { this.likes = likes; }
    public Integer getDislikes() { return dislikes; }
    public void setDislikes(Integer dislikes) { this.dislikes = dislikes; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getPostTitle() { return postTitle; }
    public void setPostTitle(String postTitle) { this.postTitle = postTitle; }
    public String getPostSlug() { return postSlug; }
    public void setPostSlug(String postSlug) { this.postSlug = postSlug; }
}
