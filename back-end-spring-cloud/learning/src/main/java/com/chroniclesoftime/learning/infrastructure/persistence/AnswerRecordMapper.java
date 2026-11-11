package com.chroniclesoftime.learning.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chroniclesoftime.learning.domain.model.AnswerRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AnswerRecordMapper extends BaseMapper<AnswerRecord> {}
