package com.chroniclesoftime.archive.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chroniclesoftime.archive.domain.model.CourseTreeNode;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CourseTreeNodeMapper extends BaseMapper<CourseTreeNode> {}
