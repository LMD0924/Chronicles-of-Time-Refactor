package com.chroniclesoftime.learning.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;

/** 学习 · 领域实体 / 持久化模型。 */
@TableName("answer_records")
public class AnswerRecord {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;
    @TableField("question_id")
    private Long questionId;
    @TableField("user_answer")
    private String userAnswer;
    @TableField("correct")
    private Boolean correct;
    @TableField("score")
    private Double score;
    @TableField("duration_ms")
    private Long durationMs;
    @TableField("answered_at")
    private String answeredAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long v) { this.userId = v; }
    public Long getQuestionId() { return questionId; }
    public void setQuestionId(Long v) { this.questionId = v; }
    public String getUserAnswer() { return userAnswer; }
    public void setUserAnswer(String v) { this.userAnswer = v; }
    public Boolean getCorrect() { return correct; }
    public void setCorrect(Boolean v) { this.correct = v; }
    public Double getScore() { return score; }
    public void setScore(Double v) { this.score = v; }
    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long v) { this.durationMs = v; }
    public String getAnsweredAt() { return answeredAt; }
    public void setAnsweredAt(String v) { this.answeredAt = v; }
}
