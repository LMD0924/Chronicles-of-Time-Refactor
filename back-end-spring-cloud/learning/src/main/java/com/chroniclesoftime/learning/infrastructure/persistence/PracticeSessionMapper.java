package com.chroniclesoftime.learning.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chroniclesoftime.learning.domain.model.PracticeSession;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PracticeSessionMapper extends BaseMapper<PracticeSession> {}
