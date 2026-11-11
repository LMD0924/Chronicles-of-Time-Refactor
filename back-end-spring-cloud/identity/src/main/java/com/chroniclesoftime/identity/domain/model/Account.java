package com.chroniclesoftime.identity.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;

/** 身份与访问 · 领域实体 / 持久化模型。 */
@TableName("account")
public class Account {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("username")
    private String username;
    @JsonIgnore
    @TableField("password_hash")
    private String passwordHash;
    @TableField("nickname")
    private String nickname;
    @TableField("role")
    private String role;
    @TableField("status")
    private String status;
    @TableField("token_version")
    private Integer tokenVersion;
    @TableField("avatar_url")
    private String avatarUrl;
    @TableField("bio")
    private String bio;
    @TableField("region")
    private String region;
    @TableField("created_at")
    private String createdAt;
    @TableField("last_login_at")
    private String lastLoginAt;
    @JsonIgnore
    @TableField("login_fail_count")
    private Integer loginFailCount;
    @JsonIgnore
    @TableField("locked_until")
    private Long lockedUntil;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String v) { this.username = v; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String v) { this.passwordHash = v; }
    public String getNickname() { return nickname; }
    public void setNickname(String v) { this.nickname = v; }
    public String getRole() { return role; }
    public void setRole(String v) { this.role = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public Integer getTokenVersion() { return tokenVersion; }
    public void setTokenVersion(Integer v) { this.tokenVersion = v; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String v) { this.avatarUrl = v; }
    public String getBio() { return bio; }
    public void setBio(String v) { this.bio = v; }
    public String getRegion() { return region; }
    public void setRegion(String v) { this.region = v; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String v) { this.createdAt = v; }
    public String getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(String v) { this.lastLoginAt = v; }
    public Integer getLoginFailCount() { return loginFailCount; }
    public void setLoginFailCount(Integer v) { this.loginFailCount = v; }
    public Long getLockedUntil() { return lockedUntil; }
    public void setLockedUntil(Long v) { this.lockedUntil = v; }
}
