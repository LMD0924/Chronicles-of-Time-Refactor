package com.chroniclesoftime.planning.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;

/** 规划 · 领域实体 / 持久化模型。 */
@TableName("volunteer_detail")
public class VolunteerDetail {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;
    @TableField("plan_id")
    private Long planId;
    @TableField("university_name")
    private String universityName;
    @TableField("major_name")
    private String majorName;
    @TableField("priority")
    private Integer priority;
    @TableField("chance_level")
    private String chanceLevel;
    @TableField("score_line")
    private Double scoreLine;
    @TableField("note")
    private String note;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long v) { this.userId = v; }
    public Long getPlanId() { return planId; }
    public void setPlanId(Long v) { this.planId = v; }
    public String getUniversityName() { return universityName; }
    public void setUniversityName(String v) { this.universityName = v; }
    public String getMajorName() { return majorName; }
    public void setMajorName(String v) { this.majorName = v; }
    public Integer getPriority() { return priority; }
    public void setPriority(Integer v) { this.priority = v; }
    public String getChanceLevel() { return chanceLevel; }
    public void setChanceLevel(String v) { this.chanceLevel = v; }
    public Double getScoreLine() { return scoreLine; }
    public void setScoreLine(Double v) { this.scoreLine = v; }
    public String getNote() { return note; }
    public void setNote(String v) { this.note = v; }
}
