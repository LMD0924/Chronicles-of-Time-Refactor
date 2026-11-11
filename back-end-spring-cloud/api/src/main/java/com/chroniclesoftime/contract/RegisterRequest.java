package com.chroniclesoftime.contract;

/** 跨限界上下文共享的 DTO 契约。 */
public class RegisterRequest {
    private String username;
    private String password;
    private String nickname;

    public String getUsername() { return username; }
    public void setUsername(String v) { this.username = v; }
    public String getPassword() { return password; }
    public void setPassword(String v) { this.password = v; }
    public String getNickname() { return nickname; }
    public void setNickname(String v) { this.nickname = v; }
}
