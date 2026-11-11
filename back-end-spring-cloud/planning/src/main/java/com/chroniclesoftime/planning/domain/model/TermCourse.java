package com.chroniclesoftime.planning.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;

/** 规划 · 领域实体 / 持久化模型。 */
@TableName("term_course")
public class TermCourse {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;
    @TableField("term")
    private String term;
    @TableField("course_name")
    private String courseName;
    @TableField("credit")
    private Double credit;
    @TableField("score")
    private Double score;
    @TableField("gpa_point")
    private Double gpaPoint;
    @TableField("category")
    private String category;
    @TableField("created_at")
    private String createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long v) { this.userId = v; }
    public String getTerm() { return term; }
    public void setTerm(String v) { this.term = v; }
    public String getCourseName() { return courseName; }
    public void setCourseName(String v) { this.courseName = v; }
    public Double getCredit() { return credit; }
    public void setCredit(Double v) { this.credit = v; }
    public Double getScore() { return score; }
    public void setScore(Double v) { this.score = v; }
    public Double getGpaPoint() { return gpaPoint; }
    public void setGpaPoint(Double v) { this.gpaPoint = v; }
    public String getCategory() { return category; }
    public void setCategory(String v) { this.category = v; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String v) { this.createdAt = v; }
}
