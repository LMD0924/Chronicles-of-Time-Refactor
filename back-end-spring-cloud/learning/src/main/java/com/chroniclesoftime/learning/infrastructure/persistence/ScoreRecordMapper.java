package com.chroniclesoftime.learning.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chroniclesoftime.learning.domain.model.ScoreRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ScoreRecordMapper extends BaseMapper<ScoreRecord> {}
