package com.chroniclesoftime.archive.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chroniclesoftime.archive.domain.model.Certificate;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CertificateMapper extends BaseMapper<Certificate> {}
