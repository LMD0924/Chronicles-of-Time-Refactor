package com.chroniclesoftime.planning.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;

/** 规划 · 领域实体 / 持久化模型。 */
@TableName("subject_combination")
public class SubjectCombination {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;
    @TableField("first_subject")
    private String firstSubject;
    @TableField("second_subjects")
    private String secondSubjects;
    @TableField("label")
    private String label;
    @TableField("student_count")
    private Integer studentCount;
    @TableField("coverage")
    private Double coverage;
    @TableField("created_at")
    private String createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long v) { this.userId = v; }
    public String getFirstSubject() { return firstSubject; }
    public void setFirstSubject(String v) { this.firstSubject = v; }
    public String getSecondSubjects() { return secondSubjects; }
    public void setSecondSubjects(String v) { this.secondSubjects = v; }
    public String getLabel() { return label; }
    public void setLabel(String v) { this.label = v; }
    public Integer getStudentCount() { return studentCount; }
    public void setStudentCount(Integer v) { this.studentCount = v; }
    public Double getCoverage() { return coverage; }
    public void setCoverage(Double v) { this.coverage = v; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String v) { this.createdAt = v; }
}
