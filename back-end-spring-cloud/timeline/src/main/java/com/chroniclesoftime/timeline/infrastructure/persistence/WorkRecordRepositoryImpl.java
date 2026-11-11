package com.chroniclesoftime.timeline.infrastructure.persistence;

import com.chroniclesoftime.timeline.domain.model.WorkRecord;
import com.chroniclesoftime.timeline.domain.repository.WorkRecordRepository;
import com.chroniclesoftime.shared.BaseMapperRepository;
import org.springframework.stereotype.Repository;

@Repository
public class WorkRecordRepositoryImpl extends BaseMapperRepository<WorkRecord, WorkRecordMapper> implements WorkRecordRepository {
    public WorkRecordRepositoryImpl(WorkRecordMapper mapper) { super(mapper); }
}
