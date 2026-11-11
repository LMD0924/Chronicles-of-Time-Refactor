package com.chroniclesoftime.learning.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;

/** 学习 · 领域实体 / 持久化模型。 */
@TableName("question_bank")
public class Question {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;
    @TableField("subject")
    private String subject;
    @TableField("knowledge_point")
    private String knowledgePoint;
    @TableField("title")
    private String title;
    @TableField("content")
    private String content;
    @TableField("options_json")
    private String optionsJson;
    @TableField("answer")
    private String answer;
    @TableField("explanation")
    private String explanation;
    @TableField("difficulty")
    private String difficulty;
    @TableField("source")
    private String source;
    @TableField("created_at")
    private String createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long v) { this.userId = v; }
    public String getSubject() { return subject; }
    public void setSubject(String v) { this.subject = v; }
    public String getKnowledgePoint() { return knowledgePoint; }
    public void setKnowledgePoint(String v) { this.knowledgePoint = v; }
    public String getTitle() { return title; }
    public void setTitle(String v) { this.title = v; }
    public String getContent() { return content; }
    public void setContent(String v) { this.content = v; }
    public String getOptionsJson() { return optionsJson; }
    public void setOptionsJson(String v) { this.optionsJson = v; }
    public String getAnswer() { return answer; }
    public void setAnswer(String v) { this.answer = v; }
    public String getExplanation() { return explanation; }
    public void setExplanation(String v) { this.explanation = v; }
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String v) { this.difficulty = v; }
    public String getSource() { return source; }
    public void setSource(String v) { this.source = v; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String v) { this.createdAt = v; }
}
