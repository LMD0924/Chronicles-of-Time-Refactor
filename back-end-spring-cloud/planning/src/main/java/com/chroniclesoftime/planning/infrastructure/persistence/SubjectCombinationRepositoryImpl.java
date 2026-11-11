package com.chroniclesoftime.planning.infrastructure.persistence;

import com.chroniclesoftime.planning.domain.model.SubjectCombination;
import com.chroniclesoftime.planning.domain.repository.SubjectCombinationRepository;
import com.chroniclesoftime.shared.BaseMapperRepository;
import org.springframework.stereotype.Repository;

@Repository
public class SubjectCombinationRepositoryImpl extends BaseMapperRepository<SubjectCombination, SubjectCombinationMapper> implements SubjectCombinationRepository {
    public SubjectCombinationRepositoryImpl(SubjectCombinationMapper mapper) { super(mapper); }
}
