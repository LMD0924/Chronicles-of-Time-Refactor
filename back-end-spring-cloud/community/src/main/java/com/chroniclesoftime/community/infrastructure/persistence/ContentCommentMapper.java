package com.chroniclesoftime.community.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chroniclesoftime.community.domain.model.ContentComment;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ContentCommentMapper extends BaseMapper<ContentComment> {}
