package com.chroniclesoftime.timeline.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;

/** 拾光时间线 · 领域实体 / 持久化模型。 */
@TableName("life_record")
public class LifeRecord {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;
    @TableField("record_date")
    private String recordDate;
    @TableField("title")
    private String title;
    @TableField("content")
    private String content;
    @TableField("category")
    private String category;
    @TableField("period")
    private String period;
    @TableField("mood")
    private String mood;
    @TableField("study_hours")
    private Double studyHours;
    @TableField("sleep_hours")
    private Double sleepHours;
    @TableField("exercise_hours")
    private Double exerciseHours;
    @TableField("happiness_index")
    private Integer happinessIndex;
    @TableField("is_milestone")
    private Boolean isMilestone;
    @TableField("tags")
    private String tags;
    @TableField("created_at")
    private String createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long v) { this.userId = v; }
    public String getRecordDate() { return recordDate; }
    public void setRecordDate(String v) { this.recordDate = v; }
    public String getTitle() { return title; }
    public void setTitle(String v) { this.title = v; }
    public String getContent() { return content; }
    public void setContent(String v) { this.content = v; }
    public String getCategory() { return category; }
    public void setCategory(String v) { this.category = v; }
    public String getPeriod() { return period; }
    public void setPeriod(String v) { this.period = v; }
    public String getMood() { return mood; }
    public void setMood(String v) { this.mood = v; }
    public Double getStudyHours() { return studyHours; }
    public void setStudyHours(Double v) { this.studyHours = v; }
    public Double getSleepHours() { return sleepHours; }
    public void setSleepHours(Double v) { this.sleepHours = v; }
    public Double getExerciseHours() { return exerciseHours; }
    public void setExerciseHours(Double v) { this.exerciseHours = v; }
    public Integer getHappinessIndex() { return happinessIndex; }
    public void setHappinessIndex(Integer v) { this.happinessIndex = v; }
    public Boolean getIsMilestone() { return isMilestone; }
    public void setIsMilestone(Boolean v) { this.isMilestone = v; }
    public String getTags() { return tags; }
    public void setTags(String v) { this.tags = v; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String v) { this.createdAt = v; }
}
