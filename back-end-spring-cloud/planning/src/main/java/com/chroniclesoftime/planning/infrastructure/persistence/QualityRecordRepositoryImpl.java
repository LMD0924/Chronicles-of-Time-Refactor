package com.chroniclesoftime.planning.infrastructure.persistence;

import com.chroniclesoftime.planning.domain.model.QualityRecord;
import com.chroniclesoftime.planning.domain.repository.QualityRecordRepository;
import com.chroniclesoftime.shared.BaseMapperRepository;
import org.springframework.stereotype.Repository;

@Repository
public class QualityRecordRepositoryImpl extends BaseMapperRepository<QualityRecord, QualityRecordMapper> implements QualityRecordRepository {
    public QualityRecordRepositoryImpl(QualityRecordMapper mapper) { super(mapper); }
}
