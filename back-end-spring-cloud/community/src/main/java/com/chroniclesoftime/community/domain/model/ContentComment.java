package com.chroniclesoftime.community.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;

/** 社区 · 领域实体 / 持久化模型。 */
@TableName("content_comment")
public class ContentComment {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;
    @TableField("content_id")
    private Long contentId;
    @TableField("author_name")
    private String authorName;
    @TableField("body")
    private String body;
    @TableField("created_at")
    private String createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long v) { this.userId = v; }
    public Long getContentId() { return contentId; }
    public void setContentId(Long v) { this.contentId = v; }
    public String getAuthorName() { return authorName; }
    public void setAuthorName(String v) { this.authorName = v; }
    public String getBody() { return body; }
    public void setBody(String v) { this.body = v; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String v) { this.createdAt = v; }
}
