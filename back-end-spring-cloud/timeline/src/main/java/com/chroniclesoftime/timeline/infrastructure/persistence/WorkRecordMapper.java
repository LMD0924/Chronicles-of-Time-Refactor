package com.chroniclesoftime.timeline.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chroniclesoftime.timeline.domain.model.WorkRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WorkRecordMapper extends BaseMapper<WorkRecord> {}
