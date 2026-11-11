package com.chroniclesoftime.learning.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chroniclesoftime.learning.domain.model.Question;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QuestionMapper extends BaseMapper<Question> {}
