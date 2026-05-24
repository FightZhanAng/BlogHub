package com.blog.dto;

public class LoginResponse {

    private String token;
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
