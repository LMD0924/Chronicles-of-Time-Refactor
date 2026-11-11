package com.chroniclesoftime.archive.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;

/** 档案 · 领域实体 / 持久化模型。 */
@TableName("certificate")
public class Certificate {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;
    @TableField("name")
    private String name;
    @TableField("issuer")
    private String issuer;
    @TableField("issued_at")
    private String issuedAt;
    @TableField("file_url")
    private String fileUrl;
    @TableField("category")
    private String category;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long v) { this.userId = v; }
    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public String getIssuer() { return issuer; }
    public void setIssuer(String v) { this.issuer = v; }
    public String getIssuedAt() { return issuedAt; }
    public void setIssuedAt(String v) { this.issuedAt = v; }
    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String v) { this.fileUrl = v; }
    public String getCategory() { return category; }
    public void setCategory(String v) { this.category = v; }
}
