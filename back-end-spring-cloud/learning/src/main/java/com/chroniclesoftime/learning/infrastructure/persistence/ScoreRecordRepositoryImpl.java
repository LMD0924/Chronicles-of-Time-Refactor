package com.chroniclesoftime.learning.infrastructure.persistence;

import com.chroniclesoftime.learning.domain.model.ScoreRecord;
import com.chroniclesoftime.learning.domain.repository.ScoreRecordRepository;
import com.chroniclesoftime.shared.BaseMapperRepository;
import org.springframework.stereotype.Repository;

@Repository
public class ScoreRecordRepositoryImpl extends BaseMapperRepository<ScoreRecord, ScoreRecordMapper> implements ScoreRecordRepository {
    public ScoreRecordRepositoryImpl(ScoreRecordMapper mapper) { super(mapper); }
}
