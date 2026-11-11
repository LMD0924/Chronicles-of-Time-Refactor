package com.chroniclesoftime.community.infrastructure.persistence;

import com.chroniclesoftime.community.domain.model.LikeRecord;
import com.chroniclesoftime.community.domain.repository.LikeRecordRepository;
import com.chroniclesoftime.shared.BaseMapperRepository;
import org.springframework.stereotype.Repository;

@Repository
public class LikeRecordRepositoryImpl extends BaseMapperRepository<LikeRecord, LikeRecordMapper> implements LikeRecordRepository {
    public LikeRecordRepositoryImpl(LikeRecordMapper mapper) { super(mapper); }
}
