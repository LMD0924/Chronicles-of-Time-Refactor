package com.chroniclesoftime.learning.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;

/** 学习 · 领域实体 / 持久化模型。 */
@TableName("score_record")
public class ScoreRecord {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;
    @TableField("subject")
    private String subject;
    @TableField("exam_name")
    private String examName;
    @TableField("exam_date")
    private String examDate;
    @TableField("score")
    private Double score;
    @TableField("full_score")
    private Double fullScore;
    @TableField("class_avg")
    private Double classAvg;
    @TableField("created_at")
    private String createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long v) { this.userId = v; }
    public String getSubject() { return subject; }
    public void setSubject(String v) { this.subject = v; }
    public String getExamName() { return examName; }
    public void setExamName(String v) { this.examName = v; }
    public String getExamDate() { return examDate; }
    public void setExamDate(String v) { this.examDate = v; }
    public Double getScore() { return score; }
    public void setScore(Double v) { this.score = v; }
    public Double getFullScore() { return fullScore; }
    public void setFullScore(Double v) { this.fullScore = v; }
    public Double getClassAvg() { return classAvg; }
    public void setClassAvg(Double v) { this.classAvg = v; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String v) { this.createdAt = v; }
}
