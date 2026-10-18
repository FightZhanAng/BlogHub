package com.blog.dto;

public class LoginResponse {

    private String token;
    /** 用于换取新 token 的刷新令牌（之前没有该字段，前端永远拿不到 refreshToken） */
    private String refreshToken;
    private String username;
    private String nickname;
    private String avatar;
    private String role;
    private Long userId;

    public LoginResponse() {}

    public LoginResponse(String token, String username, String nickname, String avatar, String role, Long userId) {
        this.token = token;
        this.username = username;
        this.nickname = nickname;
        this.avatar = avatar;
        this.role = role;
        this.userId = userId;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
}
