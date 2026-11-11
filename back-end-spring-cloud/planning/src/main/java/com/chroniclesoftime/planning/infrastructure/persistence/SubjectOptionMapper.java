package com.chroniclesoftime.planning.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chroniclesoftime.planning.domain.model.SubjectOption;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SubjectOptionMapper extends BaseMapper<SubjectOption> {}
