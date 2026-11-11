package com.chroniclesoftime.planning.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;

/** 规划 · 领域实体 / 持久化模型。 */
@TableName("selection_intention")
public class SelectionIntention {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;
    @TableField("target_major")
    private String targetMajor;
    @TableField("target_school")
    private String targetSchool;
    @TableField("reason")
    private String reason;
    @TableField("status")
    private String status;
    @TableField("created_at")
    private String createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long v) { this.userId = v; }
    public String getTargetMajor() { return targetMajor; }
    public void setTargetMajor(String v) { this.targetMajor = v; }
    public String getTargetSchool() { return targetSchool; }
    public void setTargetSchool(String v) { this.targetSchool = v; }
    public String getReason() { return reason; }
    public void setReason(String v) { this.reason = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String v) { this.createdAt = v; }
}
