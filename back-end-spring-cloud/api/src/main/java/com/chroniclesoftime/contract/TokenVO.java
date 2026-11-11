package com.chroniclesoftime.contract;

/** 跨限界上下文共享的 DTO 契约。 */
public class TokenVO {
    private String token;
    private Long userId;
    private String username;
    private String role;
    private String nickname;

    public String getToken() { return token; }
    public void setToken(String v) { this.token = v; }
    public Long getUserId() { return userId; }
    public void setUserId(Long v) { this.userId = v; }
    public String getUsername() { return username; }
    public void setUsername(String v) { this.username = v; }
    public String getRole() { return role; }
    public void setRole(String v) { this.role = v; }
    public String getNickname() { return nickname; }
    public void setNickname(String v) { this.nickname = v; }
}
