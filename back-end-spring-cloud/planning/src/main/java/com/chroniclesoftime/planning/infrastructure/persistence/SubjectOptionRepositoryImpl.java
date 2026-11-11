package com.chroniclesoftime.planning.infrastructure.persistence;

import com.chroniclesoftime.planning.domain.model.SubjectOption;
import com.chroniclesoftime.planning.domain.repository.SubjectOptionRepository;
import com.chroniclesoftime.shared.BaseMapperRepository;
import org.springframework.stereotype.Repository;

@Repository
public class SubjectOptionRepositoryImpl extends BaseMapperRepository<SubjectOption, SubjectOptionMapper> implements SubjectOptionRepository {
    public SubjectOptionRepositoryImpl(SubjectOptionMapper mapper) { super(mapper); }
}
