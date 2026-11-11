package com.chroniclesoftime.learning.infrastructure.persistence;

import com.chroniclesoftime.learning.domain.model.AnswerRecord;
import com.chroniclesoftime.learning.domain.repository.AnswerRecordRepository;
import com.chroniclesoftime.shared.BaseMapperRepository;
import org.springframework.stereotype.Repository;

@Repository
public class AnswerRecordRepositoryImpl extends BaseMapperRepository<AnswerRecord, AnswerRecordMapper> implements AnswerRecordRepository {
    public AnswerRecordRepositoryImpl(AnswerRecordMapper mapper) { super(mapper); }
}
