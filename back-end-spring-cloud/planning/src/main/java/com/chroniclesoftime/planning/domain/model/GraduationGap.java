package com.chroniclesoftime.planning.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;

/** 规划 · 领域实体 / 持久化模型。 */
@TableName("graduation_gap")
public class GraduationGap {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;
    @TableField("requirement_name")
    private String requirementName;
    @TableField("required_credit")
    private Double requiredCredit;
    @TableField("earned_credit")
    private Double earnedCredit;
    @TableField("status")
    private String status;
    @TableField("note")
    private String note;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long v) { this.userId = v; }
    public String getRequirementName() { return requirementName; }
    public void setRequirementName(String v) { this.requirementName = v; }
    public Double getRequiredCredit() { return requiredCredit; }
    public void setRequiredCredit(Double v) { this.requiredCredit = v; }
    public Double getEarnedCredit() { return earnedCredit; }
    public void setEarnedCredit(Double v) { this.earnedCredit = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public String getNote() { return note; }
    public void setNote(String v) { this.note = v; }
}
