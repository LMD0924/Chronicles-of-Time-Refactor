package com.chroniclesoftime.identity.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;

/** 身份与访问 · 领域实体 / 持久化模型。 */
@TableName("audit_log")
public class AuditLog {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("operator_id")
    private Long operatorId;
    @TableField("operator_name")
    private String operatorName;
    @TableField("action")
    private String action;
    @TableField("target_type")
    private String targetType;
    @TableField("target_id")
    private String targetId;
    @TableField("before_value")
    private String beforeValue;
    @TableField("after_value")
    private String afterValue;
    @TableField("created_at")
    private String createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getOperatorId() { return operatorId; }
    public void setOperatorId(Long v) { this.operatorId = v; }
    public String getOperatorName() { return operatorName; }
    public void setOperatorName(String v) { this.operatorName = v; }
    public String getAction() { return action; }
    public void setAction(String v) { this.action = v; }
    public String getTargetType() { return targetType; }
    public void setTargetType(String v) { this.targetType = v; }
    public String getTargetId() { return targetId; }
    public void setTargetId(String v) { this.targetId = v; }
    public String getBeforeValue() { return beforeValue; }
    public void setBeforeValue(String v) { this.beforeValue = v; }
    public String getAfterValue() { return afterValue; }
    public void setAfterValue(String v) { this.afterValue = v; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String v) { this.createdAt = v; }
}
