package com.chroniclesoftime.timeline.infrastructure.persistence;

import com.chroniclesoftime.timeline.domain.model.Milestone;
import com.chroniclesoftime.timeline.domain.repository.MilestoneRepository;
import com.chroniclesoftime.shared.BaseMapperRepository;
import org.springframework.stereotype.Repository;

@Repository
public class MilestoneRepositoryImpl extends BaseMapperRepository<Milestone, MilestoneMapper> implements MilestoneRepository {
    public MilestoneRepositoryImpl(MilestoneMapper mapper) { super(mapper); }
}
