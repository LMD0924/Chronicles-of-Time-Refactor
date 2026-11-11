package com.chroniclesoftime.learning.infrastructure.persistence;

import com.chroniclesoftime.learning.domain.model.MistakeRecord;
import com.chroniclesoftime.learning.domain.repository.MistakeRecordRepository;
import com.chroniclesoftime.shared.BaseMapperRepository;
import org.springframework.stereotype.Repository;

@Repository
public class MistakeRecordRepositoryImpl extends BaseMapperRepository<MistakeRecord, MistakeRecordMapper> implements MistakeRecordRepository {
    public MistakeRecordRepositoryImpl(MistakeRecordMapper mapper) { super(mapper); }
}
