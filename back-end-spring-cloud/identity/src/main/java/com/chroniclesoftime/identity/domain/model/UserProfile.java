package com.chroniclesoftime.identity.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;

/** 身份与访问 · 领域实体 / 持久化模型。 */
@TableName("user_profile")
public class UserProfile {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;
    @TableField("real_name")
    private String realName;
    @TableField("phone")
    private String phone;
    @TableField("email")
    private String email;
    @TableField("school")
    private String school;
    @TableField("bio")
    private String bio;
    @TableField("birthday")
    private String birthday;
    @TableField("updated_at")
    private String updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long v) { this.userId = v; }
    public String getRealName() { return realName; }
    public void setRealName(String v) { this.realName = v; }
    public String getPhone() { return phone; }
    public void setPhone(String v) { this.phone = v; }
    public String getEmail() { return email; }
    public void setEmail(String v) { this.email = v; }
    public String getSchool() { return school; }
    public void setSchool(String v) { this.school = v; }
    public String getBio() { return bio; }
    public void setBio(String v) { this.bio = v; }
    public String getBirthday() { return birthday; }
    public void setBirthday(String v) { this.birthday = v; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String v) { this.updatedAt = v; }
}
