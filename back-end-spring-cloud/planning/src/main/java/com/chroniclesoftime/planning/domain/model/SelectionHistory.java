package com.chroniclesoftime.planning.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;

/** 规划 · 领域实体 / 持久化模型。 */
@TableName("selection_history")
public class SelectionHistory {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;
    @TableField("selection_id")
    private Long selectionId;
    @TableField("action")
    private String action;
    @TableField("operator")
    private String operator;
    @TableField("detail")
    private String detail;
    @TableField("created_at")
    private String createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long v) { this.userId = v; }
    public Long getSelectionId() { return selectionId; }
    public void setSelectionId(Long v) { this.selectionId = v; }
    public String getAction() { return action; }
    public void setAction(String v) { this.action = v; }
    public String getOperator() { return operator; }
    public void setOperator(String v) { this.operator = v; }
    public String getDetail() { return detail; }
    public void setDetail(String v) { this.detail = v; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String v) { this.createdAt = v; }
}
