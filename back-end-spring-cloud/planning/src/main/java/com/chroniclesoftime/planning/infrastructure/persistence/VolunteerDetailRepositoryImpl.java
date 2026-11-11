package com.chroniclesoftime.planning.infrastructure.persistence;

import com.chroniclesoftime.planning.domain.model.VolunteerDetail;
import com.chroniclesoftime.planning.domain.repository.VolunteerDetailRepository;
import com.chroniclesoftime.shared.BaseMapperRepository;
import org.springframework.stereotype.Repository;

@Repository
public class VolunteerDetailRepositoryImpl extends BaseMapperRepository<VolunteerDetail, VolunteerDetailMapper> implements VolunteerDetailRepository {
    public VolunteerDetailRepositoryImpl(VolunteerDetailMapper mapper) { super(mapper); }
}
