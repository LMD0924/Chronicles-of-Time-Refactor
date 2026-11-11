package com.chroniclesoftime.timeline.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chroniclesoftime.timeline.domain.model.Milestone;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MilestoneMapper extends BaseMapper<Milestone> {}
