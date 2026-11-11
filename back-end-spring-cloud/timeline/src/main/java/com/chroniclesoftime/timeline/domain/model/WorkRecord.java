package com.chroniclesoftime.timeline.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;

/** 拾光时间线 · 领域实体 / 持久化模型。 */
@TableName("work_record")
public class WorkRecord {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;
    @TableField("company")
    private String company;
    @TableField("position")
    private String position;
    @TableField("start_date")
    private String startDate;
    @TableField("end_date")
    private String endDate;
    @TableField("content")
    private String content;
    @TableField("category")
    private String category;
    @TableField("created_at")
    private String createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long v) { this.userId = v; }
    public String getCompany() { return company; }
    public void setCompany(String v) { this.company = v; }
    public String getPosition() { return position; }
    public void setPosition(String v) { this.position = v; }
    public String getStartDate() { return startDate; }
    public void setStartDate(String v) { this.startDate = v; }
    public String getEndDate() { return endDate; }
    public void setEndDate(String v) { this.endDate = v; }
    public String getContent() { return content; }
    public void setContent(String v) { this.content = v; }
    public String getCategory() { return category; }
    public void setCategory(String v) { this.category = v; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String v) { this.createdAt = v; }
}
