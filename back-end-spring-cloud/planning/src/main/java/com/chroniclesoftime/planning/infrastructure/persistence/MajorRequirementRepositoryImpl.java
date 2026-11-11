package com.chroniclesoftime.planning.infrastructure.persistence;

import com.chroniclesoftime.planning.domain.model.MajorRequirement;
import com.chroniclesoftime.planning.domain.repository.MajorRequirementRepository;
import com.chroniclesoftime.shared.BaseMapperRepository;
import org.springframework.stereotype.Repository;

@Repository
public class MajorRequirementRepositoryImpl extends BaseMapperRepository<MajorRequirement, MajorRequirementMapper> implements MajorRequirementRepository {
    public MajorRequirementRepositoryImpl(MajorRequirementMapper mapper) { super(mapper); }
}
