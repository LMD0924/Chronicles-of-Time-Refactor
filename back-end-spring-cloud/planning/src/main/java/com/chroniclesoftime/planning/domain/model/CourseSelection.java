package com.chroniclesoftime.planning.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;

/** 规划 · 领域实体 / 持久化模型。 */
@TableName("course_selection")
public class CourseSelection {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;
    @TableField("combination_id")
    private Long combinationId;
    @TableField("status")
    private String status;
    @TableField("confirmed_at")
    private String confirmedAt;
    @TableField("note")
    private String note;
    @TableField("created_at")
    private String createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long v) { this.userId = v; }
    public Long getCombinationId() { return combinationId; }
    public void setCombinationId(Long v) { this.combinationId = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public String getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(String v) { this.confirmedAt = v; }
    public String getNote() { return note; }
    public void setNote(String v) { this.note = v; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String v) { this.createdAt = v; }
}
