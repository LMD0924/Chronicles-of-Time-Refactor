package com.chroniclesoftime.learning.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;

/** 学习 · 领域实体 / 持久化模型。 */
@TableName("practice_session")
public class PracticeSession {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;
    @TableField("title")
    private String title;
    @TableField("question_count")
    private Integer questionCount;
    @TableField("correct_count")
    private Integer correctCount;
    @TableField("started_at")
    private String startedAt;
    @TableField("finished_at")
    private String finishedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long v) { this.userId = v; }
    public String getTitle() { return title; }
    public void setTitle(String v) { this.title = v; }
    public Integer getQuestionCount() { return questionCount; }
    public void setQuestionCount(Integer v) { this.questionCount = v; }
    public Integer getCorrectCount() { return correctCount; }
    public void setCorrectCount(Integer v) { this.correctCount = v; }
    public String getStartedAt() { return startedAt; }
    public void setStartedAt(String v) { this.startedAt = v; }
    public String getFinishedAt() { return finishedAt; }
    public void setFinishedAt(String v) { this.finishedAt = v; }
}
