package com.chroniclesoftime.learning.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;

/** 学习 · 领域实体 / 持久化模型。 */
@TableName("mistake_record")
public class MistakeRecord {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;
    @TableField("question_id")
    private Long questionId;
    @TableField("knowledge_point")
    private String knowledgePoint;
    @TableField("reason")
    private String reason;
    @TableField("mastered")
    private Boolean mastered;
    @TableField("review_count")
    private Integer reviewCount;
    @TableField("last_review_at")
    private String lastReviewAt;
    @TableField("created_at")
    private String createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long v) { this.userId = v; }
    public Long getQuestionId() { return questionId; }
    public void setQuestionId(Long v) { this.questionId = v; }
    public String getKnowledgePoint() { return knowledgePoint; }
    public void setKnowledgePoint(String v) { this.knowledgePoint = v; }
    public String getReason() { return reason; }
    public void setReason(String v) { this.reason = v; }
    public Boolean getMastered() { return mastered; }
    public void setMastered(Boolean v) { this.mastered = v; }
    public Integer getReviewCount() { return reviewCount; }
    public void setReviewCount(Integer v) { this.reviewCount = v; }
    public String getLastReviewAt() { return lastReviewAt; }
    public void setLastReviewAt(String v) { this.lastReviewAt = v; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String v) { this.createdAt = v; }
}
