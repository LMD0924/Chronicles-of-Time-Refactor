package com.chroniclesoftime.planning.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;

/** 规划 · 领域实体 / 持久化模型。 */
@TableName("quality_record")
public class QualityRecord {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;
    @TableField("title")
    private String title;
    @TableField("category")
    private String category;
    @TableField("score")
    private Double score;
    @TableField("occurred_at")
    private String occurredAt;
    @TableField("description")
    private String description;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long v) { this.userId = v; }
    public String getTitle() { return title; }
    public void setTitle(String v) { this.title = v; }
    public String getCategory() { return category; }
    public void setCategory(String v) { this.category = v; }
    public Double getScore() { return score; }
    public void setScore(Double v) { this.score = v; }
    public String getOccurredAt() { return occurredAt; }
    public void setOccurredAt(String v) { this.occurredAt = v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { this.description = v; }
}
