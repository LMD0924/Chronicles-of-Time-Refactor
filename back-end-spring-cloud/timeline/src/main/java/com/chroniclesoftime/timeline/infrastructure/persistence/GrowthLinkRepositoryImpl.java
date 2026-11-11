package com.chroniclesoftime.timeline.infrastructure.persistence;

import com.chroniclesoftime.timeline.domain.model.GrowthLink;
import com.chroniclesoftime.timeline.domain.repository.GrowthLinkRepository;
import com.chroniclesoftime.shared.BaseMapperRepository;
import org.springframework.stereotype.Repository;

@Repository
public class GrowthLinkRepositoryImpl extends BaseMapperRepository<GrowthLink, GrowthLinkMapper> implements GrowthLinkRepository {
    public GrowthLinkRepositoryImpl(GrowthLinkMapper mapper) { super(mapper); }
}
