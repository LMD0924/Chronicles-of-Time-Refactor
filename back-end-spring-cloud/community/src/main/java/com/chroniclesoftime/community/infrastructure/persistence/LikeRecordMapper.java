package com.chroniclesoftime.community.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chroniclesoftime.community.domain.model.LikeRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LikeRecordMapper extends BaseMapper<LikeRecord> {}
