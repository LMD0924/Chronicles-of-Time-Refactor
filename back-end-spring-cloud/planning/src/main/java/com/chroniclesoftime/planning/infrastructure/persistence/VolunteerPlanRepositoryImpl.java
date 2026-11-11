package com.chroniclesoftime.planning.infrastructure.persistence;

import com.chroniclesoftime.planning.domain.model.VolunteerPlan;
import com.chroniclesoftime.planning.domain.repository.VolunteerPlanRepository;
import com.chroniclesoftime.shared.BaseMapperRepository;
import org.springframework.stereotype.Repository;

@Repository
public class VolunteerPlanRepositoryImpl extends BaseMapperRepository<VolunteerPlan, VolunteerPlanMapper> implements VolunteerPlanRepository {
    public VolunteerPlanRepositoryImpl(VolunteerPlanMapper mapper) { super(mapper); }
}
