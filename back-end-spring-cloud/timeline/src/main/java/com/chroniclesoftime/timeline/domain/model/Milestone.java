package com.chroniclesoftime.timeline.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;

/** 拾光时间线 · 领域实体 / 持久化模型。 */
@TableName("milestone")
public class Milestone {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;
    @TableField("title")
    private String title;
    @TableField("occurred_at")
    private String occurredAt;
    @TableField("description")
    private String description;
    @TableField("icon")
    private String icon;
    @TableField("color")
    private String color;
    @TableField("linked_record_id")
    private Long linkedRecordId;
    /** AUTO = 由成长记录自动生成；MANUAL = 手动创建或手动关联。 */
    @TableField("origin")
    private String origin;
    @TableField("created_at")
    private String createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long v) { this.userId = v; }
    public String getTitle() { return title; }
    public void setTitle(String v) { this.title = v; }
    public String getOccurredAt() { return occurredAt; }
    public void setOccurredAt(String v) { this.occurredAt = v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { this.description = v; }
    public String getIcon() { return icon; }
    public void setIcon(String v) { this.icon = v; }
    public String getColor() { return color; }
    public void setColor(String v) { this.color = v; }
    public Long getLinkedRecordId() { return linkedRecordId; }
    public void setLinkedRecordId(Long v) { this.linkedRecordId = v; }
    public String getOrigin() { return origin; }
    public void setOrigin(String v) { this.origin = v; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String v) { this.createdAt = v; }
}
