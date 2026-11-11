package com.chroniclesoftime.archive.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;

/** 档案 · 领域实体 / 持久化模型。 */
@TableName("paper")
public class Paper {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;
    @TableField("title")
    private String title;
    @TableField("abstract_text")
    private String abstractText;
    @TableField("keywords")
    private String keywords;
    @TableField("status")
    private String status;
    @TableField("advisor")
    private String advisor;
    @TableField("file_url")
    private String fileUrl;
    @TableField("created_at")
    private String createdAt;
    @TableField("updated_at")
    private String updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long v) { this.userId = v; }
    public String getTitle() { return title; }
    public void setTitle(String v) { this.title = v; }
    public String getAbstractText() { return abstractText; }
    public void setAbstractText(String v) { this.abstractText = v; }
    public String getKeywords() { return keywords; }
    public void setKeywords(String v) { this.keywords = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public String getAdvisor() { return advisor; }
    public void setAdvisor(String v) { this.advisor = v; }
    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String v) { this.fileUrl = v; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String v) { this.createdAt = v; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String v) { this.updatedAt = v; }
}
