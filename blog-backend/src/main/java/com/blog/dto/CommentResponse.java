package com.blog.dto;

import com.blog.entity.Comment;

import java.time.LocalDateTime;

/**
 * 评论响应
 */
public class CommentResponse {

    private Long id;
    private String nickname;
    private String content;
    private LocalDateTime createdAt;

    public static CommentResponse from(Comment comment) {
        if (comment == null) return null;
        CommentResponse r = new CommentResponse();
        r.setId(comment.getId());
        r.setNickname(comment.getNickname());
        r.setContent(comment.getContent());
        r.setCreatedAt(comment.getCreatedAt());
        return r;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
