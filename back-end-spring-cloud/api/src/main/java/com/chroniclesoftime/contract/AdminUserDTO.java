package com.chroniclesoftime.contract;

/** 跨限界上下文共享的 DTO 契约。 */
public class AdminUserDTO {
    private Long id;
    private String username;
    private String nickname;
    private String role;
    private String status;
    private String newPassword;

    public Long getId() { return id; }
    public void setId(Long v) { this.id = v; }
    public String getUsername() { return username; }
    public void setUsername(String v) { this.username = v; }
    public String getNickname() { return nickname; }
    public void setNickname(String v) { this.nickname = v; }
    public String getRole() { return role; }
    public void setRole(String v) { this.role = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String v) { this.newPassword = v; }
}
