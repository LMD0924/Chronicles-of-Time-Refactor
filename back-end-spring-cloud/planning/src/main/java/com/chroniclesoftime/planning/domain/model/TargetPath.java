package com.chroniclesoftime.planning.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;

/** 规划 · 领域实体 / 持久化模型。 */
@TableName("target_path")
public class TargetPath {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;
    @TableField("target_name")
    private String targetName;
    @TableField("stage")
    private String stage;
    @TableField("milestone")
    private String milestone;
    @TableField("deadline")
    private String deadline;
    @TableField("status")
    private String status;
    @TableField("note")
    private String note;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long v) { this.userId = v; }
    public String getTargetName() { return targetName; }
    public void setTargetName(String v) { this.targetName = v; }
    public String getStage() { return stage; }
    public void setStage(String v) { this.stage = v; }
    public String getMilestone() { return milestone; }
    public void setMilestone(String v) { this.milestone = v; }
    public String getDeadline() { return deadline; }
    public void setDeadline(String v) { this.deadline = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public String getNote() { return note; }
    public void setNote(String v) { this.note = v; }
}
