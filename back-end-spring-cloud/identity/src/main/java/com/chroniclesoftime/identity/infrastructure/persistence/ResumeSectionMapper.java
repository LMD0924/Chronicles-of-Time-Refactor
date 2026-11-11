package com.chroniclesoftime.identity.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chroniclesoftime.identity.domain.model.ResumeSection;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ResumeSectionMapper extends BaseMapper<ResumeSection> {}
