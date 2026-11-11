package com.chroniclesoftime.identity.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;

/** 身份与访问 · 领域实体 / 持久化模型。 */
@TableName("resume_section")
public class ResumeSection {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;
    @TableField("section_type")
    private String sectionType;
    @TableField("title")
    private String title;
    @TableField("subtitle")
    private String subtitle;
    @TableField("content")
    private String content;
    @TableField("start_date")
    private String startDate;
    @TableField("end_date")
    private String endDate;
    @TableField("sort_no")
    private Integer sortNo;
    @TableField("created_at")
    private String createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long v) { this.userId = v; }
    public String getSectionType() { return sectionType; }
    public void setSectionType(String v) { this.sectionType = v; }
    public String getTitle() { return title; }
    public void setTitle(String v) { this.title = v; }
    public String getSubtitle() { return subtitle; }
    public void setSubtitle(String v) { this.subtitle = v; }
    public String getContent() { return content; }
    public void setContent(String v) { this.content = v; }
    public String getStartDate() { return startDate; }
    public void setStartDate(String v) { this.startDate = v; }
    public String getEndDate() { return endDate; }
    public void setEndDate(String v) { this.endDate = v; }
    public Integer getSortNo() { return sortNo; }
    public void setSortNo(Integer v) { this.sortNo = v; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String v) { this.createdAt = v; }
}
