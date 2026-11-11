package com.chroniclesoftime.community.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chroniclesoftime.community.domain.model.FavoriteRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FavoriteRecordMapper extends BaseMapper<FavoriteRecord> {}
