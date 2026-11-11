package com.chroniclesoftime.timeline.infrastructure.persistence;

import com.chroniclesoftime.timeline.domain.model.AdvanceRecord;
import com.chroniclesoftime.timeline.domain.repository.AdvanceRecordRepository;
import com.chroniclesoftime.shared.BaseMapperRepository;
import org.springframework.stereotype.Repository;

@Repository
public class AdvanceRecordRepositoryImpl extends BaseMapperRepository<AdvanceRecord, AdvanceRecordMapper> implements AdvanceRecordRepository {
    public AdvanceRecordRepositoryImpl(AdvanceRecordMapper mapper) { super(mapper); }
}
