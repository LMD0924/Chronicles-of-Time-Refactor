package com.chroniclesoftime.auth;

public class AuthUser {
    private Long userId;
    private String username;
    private String role;
    private int tokenVersion;

    public AuthUser() {}
    public AuthUser(Long userId, String username, String role, int tokenVersion) {
        this.userId = userId; this.username = username; this.role = role; this.tokenVersion = tokenVersion;
    }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public int getTokenVersion() { return tokenVersion; }
    public void setTokenVersion(int tokenVersion) { this.tokenVersion = tokenVersion; }
}
