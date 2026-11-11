package com.chroniclesoftime.planning.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;

/** 规划 · 领域实体 / 持久化模型。 */
@TableName("major_requirement")
public class MajorRequirement {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("major_code")
    private String majorCode;
    @TableField("major_name")
    private String majorName;
    @TableField("category")
    private String category;
    @TableField("level")
    private String level;
    @TableField("required_subjects")
    private String requiredSubjects;
    @TableField("description")
    private String description;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getMajorCode() { return majorCode; }
    public void setMajorCode(String v) { this.majorCode = v; }
    public String getMajorName() { return majorName; }
    public void setMajorName(String v) { this.majorName = v; }
    public String getCategory() { return category; }
    public void setCategory(String v) { this.category = v; }
    public String getLevel() { return level; }
    public void setLevel(String v) { this.level = v; }
    public String getRequiredSubjects() { return requiredSubjects; }
    public void setRequiredSubjects(String v) { this.requiredSubjects = v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { this.description = v; }
}
