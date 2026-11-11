package com.chroniclesoftime.planning.infrastructure.persistence;

import com.chroniclesoftime.planning.domain.model.SelectionIntention;
import com.chroniclesoftime.planning.domain.repository.SelectionIntentionRepository;
import com.chroniclesoftime.shared.BaseMapperRepository;
import org.springframework.stereotype.Repository;

@Repository
public class SelectionIntentionRepositoryImpl extends BaseMapperRepository<SelectionIntention, SelectionIntentionMapper> implements SelectionIntentionRepository {
    public SelectionIntentionRepositoryImpl(SelectionIntentionMapper mapper) { super(mapper); }
}
