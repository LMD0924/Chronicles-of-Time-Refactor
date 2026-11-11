package com.chroniclesoftime.timeline.infrastructure.persistence;

import com.chroniclesoftime.timeline.domain.model.LifeRecord;
import com.chroniclesoftime.timeline.domain.repository.LifeRecordRepository;
import com.chroniclesoftime.shared.BaseMapperRepository;
import org.springframework.stereotype.Repository;

@Repository
public class LifeRecordRepositoryImpl extends BaseMapperRepository<LifeRecord, LifeRecordMapper> implements LifeRecordRepository {
    public LifeRecordRepositoryImpl(LifeRecordMapper mapper) { super(mapper); }
}
