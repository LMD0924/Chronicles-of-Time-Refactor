package com.chroniclesoftime.planning.infrastructure.persistence;

import com.chroniclesoftime.planning.domain.model.GraduationGap;
import com.chroniclesoftime.planning.domain.repository.GraduationGapRepository;
import com.chroniclesoftime.shared.BaseMapperRepository;
import org.springframework.stereotype.Repository;

@Repository
public class GraduationGapRepositoryImpl extends BaseMapperRepository<GraduationGap, GraduationGapMapper> implements GraduationGapRepository {
    public GraduationGapRepositoryImpl(GraduationGapMapper mapper) { super(mapper); }
}
