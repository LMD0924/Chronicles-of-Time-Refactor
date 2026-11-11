package com.chroniclesoftime.timeline.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;

/** 拾光时间线 · 领域实体 / 持久化模型。 */
@TableName("growth_link")
public class GrowthLink {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;
    @TableField("from_node")
    private String fromNode;
    @TableField("to_node")
    private String toNode;
    @TableField("relation")
    private String relation;
    @TableField("score")
    private Double score;
    @TableField("note")
    private String note;
    @TableField("created_at")
    private String createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long v) { this.userId = v; }
    public String getFromNode() { return fromNode; }
    public void setFromNode(String v) { this.fromNode = v; }
    public String getToNode() { return toNode; }
    public void setToNode(String v) { this.toNode = v; }
    public String getRelation() { return relation; }
    public void setRelation(String v) { this.relation = v; }
    public Double getScore() { return score; }
    public void setScore(Double v) { this.score = v; }
    public String getNote() { return note; }
    public void setNote(String v) { this.note = v; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String v) { this.createdAt = v; }
}
