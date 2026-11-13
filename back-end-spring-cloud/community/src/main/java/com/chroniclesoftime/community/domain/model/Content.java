package com.chroniclesoftime.community.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;

/** 社区 · 领域实体 / 持久化模型。 */
@TableName("content")
public class Content {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;
    @TableField("author_name")
    private String authorName;
    @TableField("title")
    private String title;
    @TableField("body")
    private String body;
    @TableField("summary")
    private String summary;
    @TableField("category")
    private String category;
    @TableField("tags")
    private String tags;
    @TableField("cover_url")
    private String coverUrl;
    @TableField("status")
    private String status;
    @TableField("view_count")
    private Integer viewCount;
    @TableField("like_count")
    private Integer likeCount;
    @TableField("favorite_count")
    private Integer favoriteCount;
    @TableField("created_at")
    private String createdAt;
    @TableField("updated_at")
    private String updatedAt;
    @TableField("review_note")
    private String reviewNote;
    @TableField("reviewed_by")
    private Long reviewedBy;
    @TableField("reviewed_at")
    private String reviewedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long v) { this.userId = v; }
    public String getAuthorName() { return authorName; }
    public void setAuthorName(String v) { this.authorName = v; }
    public String getTitle() { return title; }
    public void setTitle(String v) { this.title = v; }
    public String getBody() { return body; }
    public void setBody(String v) { this.body = v; }
    public String getSummary() { return summary; }
    public void setSummary(String v) { this.summary = v; }
    public String getCategory() { return category; }
    public void setCategory(String v) { this.category = v; }
    public String getTags() { return tags; }
    public void setTags(String v) { this.tags = v; }
    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String v) { this.coverUrl = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public Integer getViewCount() { return viewCount; }
    public void setViewCount(Integer v) { this.viewCount = v; }
    public Integer getLikeCount() { return likeCount; }
    public void setLikeCount(Integer v) { this.likeCount = v; }
    public Integer getFavoriteCount() { return favoriteCount; }
    public void setFavoriteCount(Integer v) { this.favoriteCount = v; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String v) { this.createdAt = v; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String v) { this.updatedAt = v; }
    public String getReviewNote() { return reviewNote; }
    public void setReviewNote(String v) { this.reviewNote = v; }
    public Long getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(Long v) { this.reviewedBy = v; }
    public String getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(String v) { this.reviewedAt = v; }
}
