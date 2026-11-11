package com.chroniclesoftime.planning.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;

/** 规划 · 领域实体 / 持久化模型。 */
@TableName("grading_scale")
public class GradingScale {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("subject_id")
    private Long subjectId;
    @TableField("level")
    private String level;
    @TableField("min_score")
    private Double minScore;
    @TableField("max_score")
    private Double maxScore;
    @TableField("ratio")
    private Double ratio;
    @TableField("active")
    private Boolean active;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getSubjectId() { return subjectId; }
    public void setSubjectId(Long v) { this.subjectId = v; }
    public String getLevel() { return level; }
    public void setLevel(String v) { this.level = v; }
    public Double getMinScore() { return minScore; }
    public void setMinScore(Double v) { this.minScore = v; }
    public Double getMaxScore() { return maxScore; }
    public void setMaxScore(Double v) { this.maxScore = v; }
    public Double getRatio() { return ratio; }
    public void setRatio(Double v) { this.ratio = v; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean v) { this.active = v; }
}
