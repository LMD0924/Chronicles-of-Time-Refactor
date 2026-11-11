package com.chroniclesoftime.planning.infrastructure.persistence;

import com.chroniclesoftime.planning.domain.model.TargetPath;
import com.chroniclesoftime.planning.domain.repository.TargetPathRepository;
import com.chroniclesoftime.shared.BaseMapperRepository;
import org.springframework.stereotype.Repository;

@Repository
public class TargetPathRepositoryImpl extends BaseMapperRepository<TargetPath, TargetPathMapper> implements TargetPathRepository {
    public TargetPathRepositoryImpl(TargetPathMapper mapper) { super(mapper); }
}
