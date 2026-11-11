package com.chroniclesoftime.planning.infrastructure.persistence;

import com.chroniclesoftime.planning.domain.model.ReviewPlan;
import com.chroniclesoftime.planning.domain.repository.ReviewPlanRepository;
import com.chroniclesoftime.shared.BaseMapperRepository;
import org.springframework.stereotype.Repository;

@Repository
public class ReviewPlanRepositoryImpl extends BaseMapperRepository<ReviewPlan, ReviewPlanMapper> implements ReviewPlanRepository {
    public ReviewPlanRepositoryImpl(ReviewPlanMapper mapper) { super(mapper); }
}
