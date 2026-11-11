package com.chroniclesoftime.archive.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;

/** 档案 · 领域实体 / 持久化模型。 */
@TableName("course_tree_node")
public class CourseTreeNode {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;
    @TableField("parent_id")
    private Long parentId;
    @TableField("name")
    private String name;
    @TableField("node_type")
    private String nodeType;
    @TableField("credit")
    private Double credit;
    @TableField("description")
    private String description;
    @TableField("sort_no")
    private Integer sortNo;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long v) { this.userId = v; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long v) { this.parentId = v; }
    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public String getNodeType() { return nodeType; }
    public void setNodeType(String v) { this.nodeType = v; }
    public Double getCredit() { return credit; }
    public void setCredit(Double v) { this.credit = v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { this.description = v; }
    public Integer getSortNo() { return sortNo; }
    public void setSortNo(Integer v) { this.sortNo = v; }
}
