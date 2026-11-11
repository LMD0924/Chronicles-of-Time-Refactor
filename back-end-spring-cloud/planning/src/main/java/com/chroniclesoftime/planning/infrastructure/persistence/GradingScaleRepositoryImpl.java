package com.chroniclesoftime.planning.infrastructure.persistence;

import com.chroniclesoftime.planning.domain.model.GradingScale;
import com.chroniclesoftime.planning.domain.repository.GradingScaleRepository;
import com.chroniclesoftime.shared.BaseMapperRepository;
import org.springframework.stereotype.Repository;

@Repository
public class GradingScaleRepositoryImpl extends BaseMapperRepository<GradingScale, GradingScaleMapper> implements GradingScaleRepository {
    public GradingScaleRepositoryImpl(GradingScaleMapper mapper) { super(mapper); }
}
