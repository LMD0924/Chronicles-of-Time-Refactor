package com.chroniclesoftime.planning.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;

/** 规划 · 领域实体 / 持久化模型。 */
@TableName("admission_simulation")
public class AdmissionSimulation {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;
    @TableField("plan_id")
    private Long planId;
    @TableField("detail_id")
    private Long detailId;
    @TableField("result")
    private String result;
    @TableField("probability")
    private Double probability;
    @TableField("analysis")
    private String analysis;
    @TableField("created_at")
    private String createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long v) { this.userId = v; }
    public Long getPlanId() { return planId; }
    public void setPlanId(Long v) { this.planId = v; }
    public Long getDetailId() { return detailId; }
    public void setDetailId(Long v) { this.detailId = v; }
    public String getResult() { return result; }
    public void setResult(String v) { this.result = v; }
    public Double getProbability() { return probability; }
    public void setProbability(Double v) { this.probability = v; }
    public String getAnalysis() { return analysis; }
    public void setAnalysis(String v) { this.analysis = v; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String v) { this.createdAt = v; }
}
