package com.chroniclesoftime.learning.infrastructure.persistence;

import com.chroniclesoftime.learning.domain.model.PracticeSession;
import com.chroniclesoftime.learning.domain.repository.PracticeSessionRepository;
import com.chroniclesoftime.shared.BaseMapperRepository;
import org.springframework.stereotype.Repository;

@Repository
public class PracticeSessionRepositoryImpl extends BaseMapperRepository<PracticeSession, PracticeSessionMapper> implements PracticeSessionRepository {
    public PracticeSessionRepositoryImpl(PracticeSessionMapper mapper) { super(mapper); }
}
