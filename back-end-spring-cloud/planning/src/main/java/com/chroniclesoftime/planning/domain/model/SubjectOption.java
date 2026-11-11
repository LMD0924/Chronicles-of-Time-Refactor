package com.chroniclesoftime.planning.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;

/** 规划 · 领域实体 / 持久化模型。 */
@TableName("subject_option")
public class SubjectOption {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("name")
    private String name;
    @TableField("category")
    private String category;
    @TableField("is_first")
    private Boolean isFirst;
    @TableField("is_required")
    private Boolean isRequired;
    @TableField("description")
    private String description;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public String getCategory() { return category; }
    public void setCategory(String v) { this.category = v; }
    public Boolean getIsFirst() { return isFirst; }
    public void setIsFirst(Boolean v) { this.isFirst = v; }
    public Boolean getIsRequired() { return isRequired; }
    public void setIsRequired(Boolean v) { this.isRequired = v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { this.description = v; }
}
